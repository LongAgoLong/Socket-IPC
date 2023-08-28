package com.leo.ipcsocket.server.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.leo.ipcsocket.bean.CacheMsgEntity;
import com.leo.ipcsocket.server.callback.IBinderCallback;
import com.leo.ipcsocket.server.callback.IServerMsgCallback;
import com.leo.ipcsocket.server.entity.ServerConfig;
import com.leo.ipcsocket.util.IpcLog;
import com.leo.ipcsocket.util.ThreadUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class IpcSocketService extends Service implements IBinderCallback {
    private boolean isServiceDestroyed = false;
    private static final String TAG = "IpcSocketService";
    private static final Object LOCK = new Object();
    private volatile ServerConfig mServerConfig;
    /**
     * 通过binder实现调用者client与Service之间的通信
     */
    private final SocketBinder mBinder = new SocketBinder();

    private volatile IServerMsgCallback iServerMsgCallback;
    /**
     * 存放实例
     */
    private final ConcurrentHashMap<String, IpcSocketInstance> ipcSocketInstancesMap
            = new ConcurrentHashMap<>();
    /**
     * 缓存的消息
     */
    private final ConcurrentHashMap<String, ArrayList<CacheMsgEntity>> mCacheMsgMap
            = new ConcurrentHashMap<>();
    private Thread serverThread;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onRegister(String pkg, IpcSocketInstance ipcSocketInstance) {
        IpcLog.i(TAG, "onRegister: pkg = " + pkg);
        ipcSocketInstancesMap.put(pkg, ipcSocketInstance);
        sendCacheMsg(pkg);
    }

    @Override
    public void onUnregister(String pkg) {
        IpcLog.i(TAG, "onUnregister: pkg = " + pkg);
        ipcSocketInstancesMap.remove(pkg);
    }

    @Override
    public void onMsgCallback(String pkgName, String msg) {
        if (iServerMsgCallback != null) {
            iServerMsgCallback.onReceive(pkgName, msg);
        }
    }

    private class TcpServer implements Runnable {

        int port;

        public TcpServer(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            ServerSocket serverSocket;
            try {
                // 监听端口
                IpcLog.i(TAG, "创建 port = " + port);
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                return;
            }
            while (!isServiceDestroyed) {
                try {
                    final Socket client = serverSocket.accept();
                    new IpcSocketInstance(client, IpcSocketService.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        isServiceDestroyed = true;
        Collection<IpcSocketInstance> socketInstanceCollection = ipcSocketInstancesMap.values();
        for (IpcSocketInstance ipcSocketInstance : socketInstanceCollection) {
            ipcSocketInstance.finish();
        }
        super.onDestroy();
    }

    /**
     * 发送消息
     *
     * @param msg
     */
    public void sendMsg(String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        ThreadUtils.getExecutorService().execute(() -> {
            Collection<IpcSocketInstance> socketInstanceCollection = ipcSocketInstancesMap.values();
            for (IpcSocketInstance ipcSocketInstance : socketInstanceCollection) {
                ipcSocketInstance.send(msg);
            }
        });
    }

    /**
     * 发送消息
     *
     * @param pkgName        指定包名
     * @param msg            消息
     * @param isMustBeServed 是否必须送达
     */
    public void sendMsg(String pkgName, String msg, boolean isMustBeServed) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        ThreadUtils.getExecutorService().execute(() -> {
            IpcSocketInstance ipcSocketInstance = ipcSocketInstancesMap.get(pkgName);
            if (ipcSocketInstance == null) {
                int msgEffectiveSecond = getMsgEffectiveSecond();
                if (isMustBeServed && msgEffectiveSecond > 0) {
                    synchronized (LOCK) {
                        ArrayList<CacheMsgEntity> list = mCacheMsgMap.get(pkgName);
                        if (list == null) {
                            list = new ArrayList<>();

                        }
                        CacheMsgEntity entity;
                        if (list.size() > getMaxCacheMsgCount()) {
                            entity = list.remove(0);
                            IpcLog.w(TAG, "Remove invalidation message : " + entity.toString());
                            entity.creationTime = SystemClock.elapsedRealtime();
                            entity.msg = msg;
                        } else {
                            entity = new CacheMsgEntity(msg);
                        }
                        list.add(entity);
                        mCacheMsgMap.put(pkgName, list);
                    }
                }
            } else {
                ipcSocketInstance.send(msg);
            }
        });
    }

    /**
     * 发送缓存消息
     *
     * @param pkgName
     */
    private void sendCacheMsg(String pkgName) {
        synchronized (LOCK) {
            ArrayList<CacheMsgEntity> list = mCacheMsgMap.remove(pkgName);
            if (list == null || list.isEmpty()) {
                return;
            }
            int msgEffectiveSecond = getMsgEffectiveSecond();
            IpcLog.i(TAG, "msgEffectiveSecond = " + msgEffectiveSecond);
            long realTime = SystemClock.elapsedRealtime();
            for (CacheMsgEntity msg : list) {
                if (Math.abs(realTime - msg.creationTime) / 1000 > msgEffectiveSecond) {
                    continue;
                }
                ThreadUtils.getExecutorService().execute(() -> {
                    IpcSocketInstance ipcSocketInstance = ipcSocketInstancesMap.get(pkgName);
                    if (ipcSocketInstance != null) {
                        ipcSocketInstance.send(msg.msg);
                    }
                });
            }
        }
    }

    /**
     * 发送消息给指定包名
     *
     * @param pkg
     * @param msg
     */
    public void sendMsg(String pkg, String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        if (TextUtils.isEmpty(pkg)) {
            sendMsg(msg);
            return;
        }
        ThreadUtils.getExecutorService().execute(() -> {
            IpcSocketInstance ipcSocketInstance = ipcSocketInstancesMap.get(pkg);
            if (ipcSocketInstance != null) {
                ipcSocketInstance.send(msg);
            }
        });
    }

    /**
     * 开启socket服务
     */
    public synchronized void startSocketServer() {
        if (serverThread == null
                || serverThread.isInterrupted()
                || !serverThread.isAlive()) {
            if (mServerConfig == null) {
                IpcLog.e(TAG, "startSocketServer: mServerConfig is null!!!");
                return;
            }
            serverThread = new Thread(new TcpServer(mServerConfig.port));
            serverThread.setPriority(Thread.MAX_PRIORITY);
            serverThread.start();
        }
    }

    /**
     * 设置消息回调
     *
     * @param iServerMsgCallback
     */
    public void setMsgCallback(IServerMsgCallback iServerMsgCallback) {
        this.iServerMsgCallback = iServerMsgCallback;
    }

    public void setConfig(@NonNull ServerConfig config) {
        this.mServerConfig = config;
    }

    public class SocketBinder extends Binder {

        public IpcSocketService getService() {
            return IpcSocketService.this;
        }
    }

    private int getMsgEffectiveSecond() {
        return mServerConfig != null ? mServerConfig.msgEffectiveSecond : 0;
    }

    private int getMaxCacheMsgCount() {
        return mServerConfig != null ? mServerConfig.maxCacheMsgCount : 0;
    }
}