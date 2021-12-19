package com.leo.ipcsocket.server;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.collection.ArrayMap;

import com.leo.ipcsocket.util.LogUtils;
import com.leo.ipcsocket.util.SocketParams;
import com.leo.ipcsocket.util.ThreadUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;

public class IpcSocketService extends Service implements IBinderCallback {
    private boolean isServiceDestroyed = false;
    private static final String TAG = "IpcSocketService";

    /**
     * 通过binder实现调用者client与Service之间的通信
     */
    private final SocketBinder mBinder = new SocketBinder();

    private volatile IServerMsgCallback iServerMsgCallback;
    /**
     * 存放实例
     */
    private final ArrayMap<String, IpcSocketInstance> ipcSocketInstancesMap = new ArrayMap<>();

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

    public class SocketBinder extends Binder {

        public IpcSocketService getService() {
            return IpcSocketService.this;
        }
    }
}