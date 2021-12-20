package com.leo.ipcsocket.server;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;

import com.leo.ipcsocket.util.LogUtils;

import java.util.ArrayList;

public class IpcServerHelper implements ServiceConnection, IServerMsgCallback {
    private static final String TAG = "IpcServerHelper";
    private static IpcServerHelper mInstance;

    private IpcSocketService.SocketBinder socketBinder;

    /**
     * 消息有效期限
     */
    private volatile int msgEffectiveSecond;
    /**
     * 消息接收监听
     */
    private final ArrayList<IServerMsgCallback> iServerMsgCallbackList = new ArrayList<>();

    private IpcServerHelper() {
    }

    public static IpcServerHelper getInstance() {
        if (mInstance == null) {
            synchronized (IpcServerHelper.class) {
                if (mInstance == null) {
                    mInstance = new IpcServerHelper();
                }
            }
        }
        return mInstance;
    }

    /**
     * 初始化
     *
     * @param context
     * @param msgEffectiveSecond 消息有效时长，发送指定包名信息时生效，如遇应用未绑定，会加入缓存
     * @param isDebug            是否debug
     */
    public void init(Context context, int msgEffectiveSecond, boolean isDebug) {
        if (isConnected()) {
            LogUtils.e(TAG, "Already initialized.");
            return;
        }
        this.msgEffectiveSecond = msgEffectiveSecond;
        LogUtils.openLog(isDebug);
        Intent intent = new Intent(context, IpcSocketService.class);
        context.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    /**
     * 结束服务
     *
     * @param context
     */
    public void finish(Context context) {
        LogUtils.d(TAG, "finish");
        context.unbindService(this);
        context.stopService(new Intent(context, IpcSocketService.class));
    }

    private boolean isConnected() {
        return socketBinder != null;
    }

    /**
     * 添加监听
     *
     * @param iServerMsgCallback
     */
    public void addMsgCallback(IServerMsgCallback iServerMsgCallback) {
        if (iServerMsgCallback == null || iServerMsgCallbackList.contains(iServerMsgCallback)) {
            return;
        }
        this.iServerMsgCallbackList.add(iServerMsgCallback);
    }

    /**
     * 移除消息监听
     *
     * @param iServerMsgCallback
     */
    public void removeMsgCallback(IServerMsgCallback iServerMsgCallback) {
        if (iServerMsgCallback == null) {
            return;
        }
        this.iServerMsgCallbackList.remove(iServerMsgCallback);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        LogUtils.d(TAG, "onServiceConnected");
        socketBinder = (IpcSocketService.SocketBinder) iBinder;
        socketBinder.getService().setMsgCallback(this);
        socketBinder.getService().setMsgEffectiveSecond(msgEffectiveSecond);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        LogUtils.d(TAG, "onServiceDisconnected");
        socketBinder = null;
    }

    public void sendMsg(@NonNull String msg) {
        if (isConnected()) {
            socketBinder.getService().sendMsg(msg);
        }
    }

    public void sendMsg(@NonNull String pkgName, @NonNull String msg, boolean isMustBeServed) {
        if (isConnected()) {
            socketBinder.getService().sendMsg(pkgName, msg, isMustBeServed);
        }
    }

    @Override
    public void onReceive(String pkgName, String msg) {
        for (IServerMsgCallback iServerMsgCallback : iServerMsgCallbackList) {
            iServerMsgCallback.onReceive(pkgName, msg);
        }
    }
}
