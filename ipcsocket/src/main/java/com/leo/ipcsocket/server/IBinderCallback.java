package com.leo.ipcsocket.server;

public interface IBinderCallback {
    void onRegister(String pkg, IpcSocketInstance ipcSocketInstance);

    void onUnregister(String pkg);

    void onMsgCallback(String pkg, String msg);
}
