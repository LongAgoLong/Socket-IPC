package com.leo.ipcsocket.server;

public interface IServerMsgCallback {

    void onReceive(String pkgName, String msg);
}
