package com.leo.ipcsocket.bean;

import android.os.SystemClock;

public class CacheMsgEntity {
    public long creationTime;
    public String msg;

    public CacheMsgEntity(String msg) {
        this.msg = msg;
        this.creationTime = SystemClock.elapsedRealtime();
    }

    @Override
    public String toString() {
        return "{\"creationTime\":" + creationTime + ",\"msg\":" + msg + "}";
    }
}
