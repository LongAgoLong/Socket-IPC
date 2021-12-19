package com.leo.socketclient;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.leo.ipcsocket.client.IpcClientHelper;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "client";
    private TextView msgTv;
    private EditText etReceive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button bindSocketBtn = findViewById(R.id.bindSocketBtn);
        bindSocketBtn.setOnClickListener(view -> {
            IpcClientHelper.getInstance().addMsgCallback(msg -> runOnUiThread(() -> msgTv.append("服务端：\n" + msg + "\n")));
            IpcClientHelper.getInstance().init(true);
        });

        Button sendMsgBtn = findViewById(R.id.sendMsgBtn);
        sendMsgBtn.setOnClickListener(view -> {
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
        super.onDestroy();
        IpcClientHelper.getInstance().finish();
    }
}