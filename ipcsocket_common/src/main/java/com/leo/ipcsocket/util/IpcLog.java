package com.leo.ipcsocket.util;

import android.util.Log;

public class IpcLog {
    private static final String TAG_ = "IPCSocket-";
    private static int type = Log.VERBOSE;

    public static void openLog(boolean isOpen) {
        type = isOpen ? Log.VERBOSE : Log.ERROR;
    }

    public void v(String tag, String msg) {
        if (type <= Log.VERBOSE) {
            // 控制台输出
            if (null != msg) {
                Log.v(TAG_ + tag, msg);
            } else {
                Log.v(TAG_ + tag, "the message is null");
            }
        }
    }

    public static void d(String tag, String msg) {
        if (type <= Log.DEBUG) {
            // 控制台输出
            if (null != msg) {
                Log.d(TAG_ + tag, msg);
            } else {
                Log.d(TAG_ + tag, "the message is null");
            }
        }
    }

    public static void i(String tag, String msg) {
        if (type <= Log.INFO) {
            // 控制台输出
            if (null != msg) {
                Log.i(TAG_ + tag, msg);
            } else {
                Log.i(TAG_ + tag, "the message is null");
            }
        }
    }

    public static void w(String tag, String msg) {
        if (type <= Log.WARN) {
            // 控制台输出
            if (null != msg) {
                Log.w(TAG_ + tag, msg);
            } else {
                Log.w(TAG_ + tag, "the message is null");
            }
        }
    }

    public static void e(String tag, String msg) {
        if (type <= Log.ERROR) {
            // 控制台输出
            if (null != msg) {
                Log.e(TAG_ + tag, msg);
            } else {
                Log.e(TAG_ + tag, "the message is null");
            }
        }
    }
}
