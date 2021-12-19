package com.leo.ipcsocket.server;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.leo.ipcsocket.protocol.RegisterPkgProtocol;
import com.leo.ipcsocket.util.IOUtils;
import com.leo.ipcsocket.util.LogUtils;
import com.leo.ipcsocket.util.ThreadUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class IpcSocketInstance {
    private static final String TAG = "IpcSocketInstance";
    private volatile boolean mIsServiceDestroyed;
    private volatile PrintWriter writer;
    private volatile String pkgName;

    private final IBinderCallback mBinderCallback;

    public IpcSocketInstance(Socket client, IBinderCallback iBinderCallback) {
        this.mBinderCallback = iBinderCallback;
        new Thread() {
            @Override
            public void run() {
                try {
                    responseClient(client);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void finish() {
        this.mIsServiceDestroyed = true;
    }

    /**
     * 接收客户端的消息
     *
     * @param client 客户端
     * @throws IOException
     */
    private void responseClient(Socket client) throws IOException {
        // 接收客户端消息
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream()));
        // 向客户端发送消息
        writer = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(client.getOutputStream())), true);

        while (!mIsServiceDestroyed) {
            try {
                String msg = reader.readLine();
                LogUtils.i(TAG, "receiver: msg = " + msg);
                if (msg == null) {
                    break;
                }
                if (mBinderCallback != null) {
                    if (!TextUtils.isEmpty(this.pkgName)) {
                        mBinderCallback.onMsgCallback(this.pkgName, msg);
                    } else {
                        String pkgName = isRegisterPkgInfo(msg);
                        if (!TextUtils.isEmpty(pkgName)) {
                            this.pkgName = pkgName;
                            mBinderCallback.onRegister(pkgName, IpcSocketInstance.this);
                        } else {
                            LogUtils.e(TAG, "pkgName is null or empty.");
                        }
                    }
                }
            } catch (Exception | Error e) {
                e.printStackTrace();
            }
        }
        if (mBinderCallback != null) {
            mBinderCallback.onUnregister(pkgName);
        }
        // 关闭通信
        IOUtils.close(writer, reader, client);
    }

    /**
     * 是否是注册包名信息
     *
     * @param msg
     * @return
     */
    private String isRegisterPkgInfo(String msg) {
        if (TextUtils.isEmpty(msg)) {
            return "";
        }
        try {
            JSONObject jsonObject = JSON.parseObject(msg);
            String nameSpace = jsonObject.getString("nameSpace");
            String name = jsonObject.getString("name");
            if (RegisterPkgProtocol.NAME_SPACE.equals(nameSpace)
                    && RegisterPkgProtocol.NAME.equals(name)) {
                JSONObject data = jsonObject.getJSONObject("data");
                return data.getString("pkgName");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 发送消息
     *
     * @param msg
     */
    public void send(String msg) {
        if (writer == null) {
            Log.e(TAG, "outWriter is null.");
            return;
        }
        ThreadUtils.getInstance().getExecutorService().execute(() -> {
            if (writer != null) {
                writer.println(msg);
            }
        });
    }
}
