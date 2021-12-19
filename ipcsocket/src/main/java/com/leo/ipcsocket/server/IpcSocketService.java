package com.leo.ipcsocket.server;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;

import com.leo.ipcsocket.util.IOUtils;
import com.leo.ipcsocket.util.LogUtils;
import com.leo.ipcsocket.util.SocketParams;
import com.leo.ipcsocket.util.ThreadUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class IpcSocketService extends Service {
    private boolean isServiceDestroyed = false;
    private static final String TAG = "IpcSocketService";

    /**
     * 通过binder实现调用者client与Service之间的通信
     */
    private final SocketBinder mBinder = new SocketBinder();
    private volatile PrintWriter printWriter;

    private volatile IServerMsgCallback iServerMsgCallback;

    @Override
    public void onCreate() {
        new Thread(new TcpServer()).start();
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private class TcpServer implements Runnable {
        @Override
        public void run() {
            ServerSocket serverSocket;
            try {
                // 监听端口
                serverSocket = new ServerSocket(SocketParams.PORT);
            } catch (IOException e) {
                return;
            }
            while (!isServiceDestroyed) {
                try {
                    // 接受客户端请求，并且阻塞直到接收到消息
                    final Socket client = serverSocket.accept();
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void responseClient(Socket client) throws IOException {
        // 用于接收客户端消息
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        // 用于向客户端发送消息
        printWriter = new PrintWriter(new BufferedWriter(new
                OutputStreamWriter(client.getOutputStream())), true);
        while (!isServiceDestroyed) {
            String str = in.readLine();
            if (TextUtils.isEmpty(str)) {
                // 客户端断开了连接
                break;
            }
            if (iServerMsgCallback != null) {
                iServerMsgCallback.onReceive(str);
            }
            LogUtils.i(TAG, str);
        }
        IOUtils.close(printWriter, in, client);
    }

    @Override
    public void onDestroy() {
        isServiceDestroyed = true;
        super.onDestroy();
    }

    /**
     * 发送消息
     *
     * @param msg
     */
    public void sendMsg(String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        ThreadUtils.getInstance().getExecutorService().execute(() -> {
            if (printWriter != null) {
                printWriter.println(msg);
            }
        });
    }

    public void setMsgCallback(IServerMsgCallback iServerMsgCallback) {
        this.iServerMsgCallback = iServerMsgCallback;
    }

    public class SocketBinder extends Binder {

        public IpcSocketService getService() {
            return IpcSocketService.this;
        }
    }
}