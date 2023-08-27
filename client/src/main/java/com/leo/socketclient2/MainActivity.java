package com.leo.socketclient2;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.leo.ipcsocket.client.ClientConfig;
import com.leo.ipcsocket.client.IClientMsgCallback;
import com.leo.ipcsocket.client.IConnectChangeCallback;
import com.leo.ipcsocket.client.IpcClientHelper;
import com.leo.ipcsocket.util.IpcLog;
import com.leo.ipcsocket.util.SocketParams;

public class MainActivity extends AppCompatActivity implements IClientMsgCallback, IConnectChangeCallback {

    private static final String TAG = "client";
    private TextView msgTv;
    private EditText etReceive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.bindSocketBtn).setOnClickListener(view -> {
            IpcClientHelper.getInstance().addMsgCallback(this);
            ClientConfig config = new ClientConfig.Builder()
                    .setMsgEffectiveSecond(10)
                    .setMaxCacheMsgCount(99)
                    .setPort(SocketParams.DEFAULT_PORT)
                    .build();
            IpcClientHelper.getInstance().init(MainActivity.this, config,
                    MainActivity.this, true);
        });

        findViewById(R.id.sendMsgBtn).setOnClickListener(view -> {
            final String msg = etReceive.getText().toString();
            // 向服务器发送消息
            if (TextUtils.isEmpty(msg)) {
                return;
            }
            IpcClientHelper.getInstance().sendMsg(msg, true);
            msgTv.append("客户端：\n" + msg + "\n");
            etReceive.setText("");
        });
        msgTv = findViewById(R.id.msgTv);
        etReceive = findViewById(R.id.etReceive);
    }

    @Override
    protected void onDestroy() {
        IpcClientHelper.getInstance().removeMsgCallback(this);
        IpcClientHelper.getInstance().finish();
        super.onDestroy();
    }

    @Override
    public void onReceive(String msg) {
        runOnUiThread(() -> {
            if (msgTv != null) {
                msgTv.append("服务端：\n" + msg + "\n");
            }
        });
    }

    @Override
    public void onConnectChangeCallback(boolean isConnect) {
        runOnUiThread(() -> {
            if (msgTv != null) {
                msgTv.append("连接状态：" + isConnect);
            }
        });
    }
}