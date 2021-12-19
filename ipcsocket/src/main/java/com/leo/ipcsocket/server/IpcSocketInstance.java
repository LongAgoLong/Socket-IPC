package com.leo.ipcsocket.server;

import android.util.Log;

import com.leo.ipcsocket.util.IOUtils;
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

    private volatile IServerMsgCallback mServerMsgCallback;

    public IpcSocketInstance(Socket client, IServerMsgCallback iServerMsgCallback) {
        this.mServerMsgCallback = iServerMsgCallback;
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
                if (msg == null) {
                    break;
                }
                if (mServerMsgCallback != null) {
                    mServerMsgCallback.onReceive(msg);
                }
            } catch (Exception | Error e) {
                e.printStackTrace();
            }
        }

        // 关闭通信
        IOUtils.close(writer, reader, client);
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
