package com.leo.ipcsocket.server;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.SystemClock;

import com.leo.ipcsocket.util.LogUtils;

public class IpcServerHelper implements ServiceConnection {
    private static final String TAG = "IpcServerHelper";
    private static IpcServerHelper mInstance;

    private IpcSocketService.SocketBinder socketBinder;

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
     * @param isDebug
     */
    public void init(Context context, boolean isDebug) {
        if (isConnected()) {
            return;
        }
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
        context.unbindService(this);
        context.stopService(new Intent(context, IpcSocketService.class));
    }

    public boolean isConnected() {
        return socketBinder != null;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        socketBinder = (IpcSocketService.SocketBinder) iBinder;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        socketBinder = null;
    }

    public void sendMsg(String msg) {
        if (socketBinder != null) {
            socketBinder.getService().sendMsg("服务端小冰测试数据：" + SystemClock.elapsedRealtime());
        }
    }
}
