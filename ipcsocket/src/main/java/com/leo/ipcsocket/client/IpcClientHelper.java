package com.leo.ipcsocket.client;

import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.leo.ipcsocket.protocol.RegisterPkgProtocol;
import com.leo.ipcsocket.util.IOUtils;
import com.leo.ipcsocket.util.LogUtils;
import com.leo.ipcsocket.util.SocketParams;
import com.leo.ipcsocket.util.ThreadUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

public class IpcClientHelper {
    private static final String TAG = "IpcClientHelper";
    private static IpcClientHelper mInstance;
    private static final Object LOCK = new Object();
    /**
     * 缓存的消息
     */
    private final ArrayList<String> mCacheMsgList = new ArrayList<>();
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
     */
    public synchronized void init(Context context, boolean isDebug) {
        if (isInit) {
            LogUtils.e(TAG, "Already initialized.");
            return;
        }
        packageName = context.getPackageName();
        isInit = true;
        isFinishing = false;
        LogUtils.openLog(isDebug);
        new Thread(this::connectSocketServer).start();
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
     * @param msg        格式消息
     * @param isMustSend 是否必须发送，true-服务未连接时缓存消息
     */
    public void sendMsg(String msg, boolean isMustSend) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        if (!isConnected()) {
            if (isMustSend) {
                cacheMsg(msg);
            }
        } else {
            ThreadUtils.getInstance().getExecutorService().execute(() -> {
                if (isConnected()) {
                    mPrintWriter.println(msg);
                } else if (isMustSend) {
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
            LogUtils.d(TAG, "cacheMsg: msg = " + msg);
            mCacheMsgList.add(msg);
        }
    }

    /**
     * 连接TCP服务器
     */
    private void connectSocketServer() {
        int counter = 0;
        Socket socket = null;
        while (socket == null) {
            try {
                // 选择和服务器相同的端口
                socket = new Socket("localhost", SocketParams.PORT);
                mPrintWriter = new PrintWriter(new BufferedWriter(new
                        OutputStreamWriter(socket.getOutputStream())), true);
                LogUtils.i(TAG, "socket服务连接成功");
            } catch (IOException e) {
                LogUtils.e(TAG, "socket连接TCP服务失败, 重试...");
                SystemClock.sleep(1000L * (Math.min(counter++, 10)));
            }
        }
        // 发送注册pkg消息
        RegisterPkgProtocol registerPkgProtocol = new RegisterPkgProtocol(this.packageName);
        String info = JSON.toJSONString(registerPkgProtocol);
        LogUtils.i(TAG, "register: info = " + info);
        mPrintWriter.println(info);
        // 处理缓存消息
        synchronized (LOCK) {
            if (!mCacheMsgList.isEmpty()) {
                Iterator<String> iterator = mCacheMsgList.iterator();
                while (iterator.hasNext()) {
                    String s = iterator.next();
                    mPrintWriter.println(s);
                    iterator.remove();
                }
            }
        }
        try {
            // 接收服务端的消息
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (!isFinishing) {
                final String msg = br.readLine();
                LogUtils.d(TAG, "receiver: msg = " + msg);
                if (msg == null) {
                    break;
                }
                for (IClientMsgCallback iClientMsgCallback : mClientMsgCallbackList) {
                    iClientMsgCallback.onReceive(msg);
                }
            }
            IOUtils.close(mPrintWriter, br, socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 重新绑定
        connectSocketServer();
    }
}
