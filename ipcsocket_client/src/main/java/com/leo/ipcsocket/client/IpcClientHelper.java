package com.leo.ipcsocket.client;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.leo.ipcsocket.bean.CacheMsgEntity;
import com.leo.ipcsocket.client.callback.IClientMsgCallback;
import com.leo.ipcsocket.client.callback.IConnectChangeCallback;
import com.leo.ipcsocket.client.entity.ClientConfig;
import com.leo.ipcsocket.protocol.RegisterPkgProtocol;
import com.leo.ipcsocket.util.IOUtils;
import com.leo.ipcsocket.util.IpcLog;
import com.leo.ipcsocket.util.SocketParams;
import com.leo.ipcsocket.util.ThreadUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;

public class IpcClientHelper {
    private static final String TAG = "IpcClientHelper";
    private static volatile IpcClientHelper mInstance;
    private static final Object LOCK = new Object();
    private volatile IConnectChangeCallback mConnectChangeCallback;
    /**
     * 缓存的消息
     */
    private final ArrayList<CacheMsgEntity> mCacheMsgList = new ArrayList<>();
    /**
     * 是否已经初始化
     */
    private boolean isInit = false;
    /**
     * 是否结束标志位
     */
    private volatile boolean isFinishing = false;
    /**
     * 消息发送通道
     */
    private volatile PrintWriter mPrintWriter;
    /**
     * 消息接收监听
     */
    private final ArrayList<IClientMsgCallback> mClientMsgCallbackList = new ArrayList<>();
    private volatile String packageName;
    private volatile ClientConfig mClientConfig;

    private IpcClientHelper() {
    }

    public static IpcClientHelper getInstance() {
        if (mInstance == null) {
            synchronized (IpcClientHelper.class) {
                if (mInstance == null) {
                    mInstance = new IpcClientHelper();
                }
            }
        }
        return mInstance;
    }

    /**
     * 初始化
     *
     * @param context
     * @param config  配置
     * @param isDebug 是否debug
     */
    public synchronized void init(Context context, ClientConfig config,
                                  IConnectChangeCallback connectChangeCallback, boolean isDebug) {
        if (isInit) {
            IpcLog.e(TAG, "Already initialized.");
            return;
        }
        this.mClientConfig = config;
        this.mConnectChangeCallback = connectChangeCallback;
        packageName = getProcessName(context);
        isInit = true;
        isFinishing = false;
        IpcLog.openLog(isDebug);
        Thread thread = new Thread(this::connectSocketServer);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private String getProcessName(@NonNull Context context) {
        String processName = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            processName = Application.getProcessName();
            IpcLog.i(TAG, "getProcessName#Application: processName = " + processName);
        } else {
            // 通过反射ActivityThread获取进程名，避免了ipc
            try {
                final Method declaredMethod = Class.forName("android.app.ActivityThread",
                                false, Application.class.getClassLoader())
                        .getDeclaredMethod("currentProcessName", (Class<?>[]) new Class[0]);
                declaredMethod.setAccessible(true);
                final Object invoke = declaredMethod.invoke(null, new Object[0]);
                if (invoke instanceof String) {
                    processName = (String) invoke;
                    IpcLog.i(TAG, "getProcessName#reflection: processName = " + processName);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        if (TextUtils.isEmpty(processName)) {
            processName = context.getPackageName();
            IpcLog.i(TAG, "getProcessName#getPackageName: processName = " + processName);
        }
        return processName;
    }

    /**
     * 结束绑定
     */
    public void finish() {
        isInit = false;
        isFinishing = true;
    }

    /**
     * 服务是否绑定
     *
     * @return
     */
    public boolean isConnected() {
        return mPrintWriter != null;
    }

    /**
     * 添加消息回调
     *
     * @param iClientMsgCallback
     */
    public void addMsgCallback(IClientMsgCallback iClientMsgCallback) {
        if (iClientMsgCallback == null || mClientMsgCallbackList.contains(iClientMsgCallback)) {
            return;
        }
        this.mClientMsgCallbackList.add(iClientMsgCallback);
    }

    /**
     * 移除消息回调
     *
     * @param iClientMsgCallback
     */
    public void removeMsgCallback(IClientMsgCallback iClientMsgCallback) {
        if (iClientMsgCallback == null) {
            return;
        }
        this.mClientMsgCallbackList.remove(iClientMsgCallback);
    }

    /**
     * 发送消息
     *
     * @param msg 格式消息
     */
    public void sendMsg(String msg) {
        sendMsg(msg, false);
    }

    /**
     * 发送消息
     *
     * @param msg            格式消息
     * @param isMustBeServed 是否必须送达，true-服务未连接时缓存消息
     */
    public void sendMsg(String msg, boolean isMustBeServed) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        if (!isConnected()) {
            if (isMustBeServed) {
                cacheMsg(msg);
            }
        } else {
            ThreadUtils.getExecutorService().execute(() -> {
                if (isConnected()) {
                    mPrintWriter.println(msg);
                } else if (isMustBeServed) {
                    cacheMsg(msg);
                }
            });
        }
    }

    /**
     * 缓存消息
     *
     * @param msg
     */
    private void cacheMsg(String msg) {
        synchronized (LOCK) {
            int maxCacheMsgCount = getMaxCacheMsgCount();
            IpcLog.d(TAG, "cacheMsg: maxCacheMsgCount = " + maxCacheMsgCount
                    + " ; isInit = " + isInit
                    + " ; msg = " + msg);
            CacheMsgEntity entity;
            int maxCount = isInit ? maxCacheMsgCount : SocketParams.DEFAULT_CACHE_MSG_COUNT;
            if (mCacheMsgList.size() > maxCount) {
                entity = mCacheMsgList.remove(0);
                entity.creationTime = SystemClock.elapsedRealtime();
                entity.msg = msg;
            } else {
                entity = new CacheMsgEntity(msg);
            }
            mCacheMsgList.add(entity);
        }
    }

    /**
     * 连接TCP服务器
     */
    private void connectSocketServer() {
        if (mClientConfig == null) {
            IpcLog.e(TAG, "connectSocketServer: mClientConfig is null!!!");
            return;
        }
        int counter = 0;
        Socket socket = null;
        while (socket == null) {
            try {
                // 选择和服务器相同的端口
                IpcLog.i(TAG, "连接 port = " + mClientConfig.port);
                socket = new Socket("localhost", mClientConfig.port);
                mPrintWriter = new PrintWriter(new BufferedWriter(new
                        OutputStreamWriter(socket.getOutputStream())), true);
                IpcLog.i(TAG, "socket服务连接成功");
            } catch (IOException e) {
                IpcLog.e(TAG, "socket连接TCP服务失败, 重试...");
                counter++;
                counter = Math.min(counter, 10);
                SystemClock.sleep(1000L * counter);
            }
        }
        // 发送注册pkg消息
        RegisterPkgProtocol registerPkgProtocol = new RegisterPkgProtocol(this.packageName);
        String info = JSON.toJSONString(registerPkgProtocol);
        IpcLog.i(TAG, "register: info = " + info);
        mPrintWriter.println(info);
        if (mConnectChangeCallback != null) {
            mConnectChangeCallback.onConnectChangeCallback(true);
        }
        // 处理缓存消息
        synchronized (LOCK) {
            if (!mCacheMsgList.isEmpty()) {
                int msgEffectiveSecond = getMsgEffectiveSecond();
                IpcLog.i(TAG, "msgEffectiveSecond = " + msgEffectiveSecond);
                long realTime = SystemClock.elapsedRealtime();
                for (CacheMsgEntity cacheMsgEntity : mCacheMsgList) {
                    if (Math.abs(realTime - cacheMsgEntity.creationTime) / 1000 <= msgEffectiveSecond) {
                        mPrintWriter.println(cacheMsgEntity.msg);
                    }
                }
                mCacheMsgList.clear();
            }
        }
        try {
            // 接收服务端的消息
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (!isFinishing) {
                final String msg = br.readLine();
                IpcLog.d(TAG, "receiver: msg = " + msg);
                if (msg == null) {
                    break;
                }
                for (IClientMsgCallback iClientMsgCallback : mClientMsgCallbackList) {
                    iClientMsgCallback.onReceive(msg);
                }
            }
            IOUtils.close(mPrintWriter, br, socket);
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
        if (mConnectChangeCallback != null) {
            mConnectChangeCallback.onConnectChangeCallback(false);
        }
        // 非主动中断，需要重新绑定
        if (!isFinishing) {
            connectSocketServer();
        }
    }

    private int getMsgEffectiveSecond() {
        return mClientConfig != null ? mClientConfig.msgEffectiveSecond : 0;
    }

    private int getMaxCacheMsgCount() {
        return mClientConfig != null ? mClientConfig.maxCacheMsgCount : 0;
    }
}
