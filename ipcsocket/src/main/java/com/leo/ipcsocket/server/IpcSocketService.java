package com.leo.ipcsocket.server;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.collection.ArrayMap;

import com.leo.ipcsocket.bean.CacheMsgEntity;
import com.leo.ipcsocket.util.LogUtils;
import com.leo.ipcsocket.util.SocketParams;
import com.leo.ipcsocket.util.ThreadUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;

public class IpcSocketService extends Service implements IBinderCallback {
    private boolean isServiceDestroyed = false;
    private static final String TAG = "IpcSocketService";
    private static final Object LOCK = new Object();

    /**
     * 消息有效期限
     */
    private volatile long msgEffectiveSecond = 10L;
    /**
     * 通过binder实现调用者client与Service之间的通信
     */
    private final SocketBinder mBinder = new SocketBinder();

    private volatile IServerMsgCallback iServerMsgCallback;
    /**
     * 存放实例
     */
    private final ArrayMap<String, IpcSocketInstance> ipcSocketInstancesMap = new ArrayMap<>();
    /**
     * 缓存的消息
     */
    private final ArrayMap<String, ArrayList<CacheMsgEntity>> mCacheMsgMap = new ArrayMap<>();

    @Override
    public void onCreate() {
        new Thread(new TcpServer()).start();
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onRegister(String pkg, IpcSocketInstance ipcSocketInstance) {
        LogUtils.i(TAG, "onRegister: pkg = " + pkg);
        ipcSocketInstancesMap.put(pkg, ipcSocketInstance);
        sendCacheMsg(pkg);
    }

    @Override
    public void onUnregister(String pkg) {
        LogUtils.i(TAG, "onUnregister: pkg = " + pkg);
        ipcSocketInstancesMap.remove(pkg);
    }

    @Override
    public void onMsgCallback(String pkgName, String msg) {
        if (iServerMsgCallback != null) {
            iServerMsgCallback.onReceive(pkgName, msg);
        }
    }

    private class TcpServer implements Runnable {
        @Override
        public void run() {
            ServerSocket serverSocket;
            try {
                // 监听端口
                serverSocket = new ServerSocket(SocketParams.PORT);
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
        ThreadUtils.getInstance().getExecutorService().execute(() -> {
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
        ThreadUtils.getInstance().getExecutorService().execute(() -> {
            IpcSocketInstance ipcSocketInstance = ipcSocketInstancesMap.get(pkgName);
            if (ipcSocketInstance == null) {
                if (isMustBeServed && msgEffectiveSecond > 0) {
                    synchronized (LOCK) {
                        ArrayList<CacheMsgEntity> list = mCacheMsgMap.get(pkgName);
                        if (list == null) {
                            list = new ArrayList<>();

                        }
                        CacheMsgEntity entity;
                        if (list.size() > 30) {
                            entity = list.remove(0);
                            LogUtils.w(TAG, "Remove invalidation message : " + entity.toString());
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
            long realTime = SystemClock.elapsedRealtime();
            for (CacheMsgEntity msg : list) {
                if (Math.abs(realTime - msg.creationTime) / 1000 > msgEffectiveSecond) {
                    continue;
                }
                ThreadUtils.getInstance().getExecutorService().execute(() -> {
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
        ThreadUtils.getInstance().getExecutorService().execute(() -> {
            IpcSocketInstance ipcSocketInstance = ipcSocketInstancesMap.get(pkg);
            if (ipcSocketInstance != null) {
                ipcSocketInstance.send(msg);
            }
        });
    }

    /**
     * 设置消息回调
     *
     * @param iServerMsgCallback
     */
    public void setMsgCallback(IServerMsgCallback iServerMsgCallback) {
        this.iServerMsgCallback = iServerMsgCallback;
    }

    /**
     * 设置消息有效期限
     *
     * @param msgEffectiveSecond
     */
    public void setMsgEffectiveSecond(int msgEffectiveSecond) {
        this.msgEffectiveSecond = msgEffectiveSecond;
    }

    public class SocketBinder extends Binder {

        public IpcSocketService getService() {
            return IpcSocketService.this;
        }
    }
}