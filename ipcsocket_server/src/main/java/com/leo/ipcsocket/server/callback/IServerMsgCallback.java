package com.leo.ipcsocket.server.callback;

public interface IServerMsgCallback {

    void onReceive(String pkgName, String msg);
}
