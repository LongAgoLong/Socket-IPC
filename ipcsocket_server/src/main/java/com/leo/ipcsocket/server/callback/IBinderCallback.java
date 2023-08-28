package com.leo.ipcsocket.server.callback;

import com.leo.ipcsocket.server.service.IpcSocketInstance;

public interface IBinderCallback {
    void onRegister(String pkg, IpcSocketInstance ipcSocketInstance);

    void onUnregister(String pkg);

    void onMsgCallback(String pkg, String msg);
}
