package com.leo.socketipc;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.leo.ipcsocket.server.IpcServerHelper;
import com.leo.ipcsocket.server.ServerConfig;
import com.leo.ipcsocket.util.SocketParams;

public class MainActivity extends AppCompatActivity {

    private EditText etSend;
    private TextView msgTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IpcServerHelper.getInstance().addMsgCallback((pkgName, msg) -> {
            runOnUiThread(() -> msgTv.append(pkgName + "：\n" + msg + "\n"));
        });
        ServerConfig config = new ServerConfig.Builder()
                .setMsgEffectiveSecond(10)
                .setMaxCacheMsgCount(99)
                .setPort(SocketParams.DEFAULT_PORT)
                .build();
        IpcServerHelper.getInstance().init(this, config, true);

        msgTv = findViewById(R.id.msgTv);
        etSend = findViewById(R.id.etSend);
        findViewById(R.id.sendMsgBtn).setOnClickListener(view -> {
            final String msg = etSend.getText().toString();
            // 向服务器发送消息
            if (TextUtils.isEmpty(msg)) {
                return;
            }
            IpcServerHelper.getInstance().sendMsg(msg);
            msgTv.append("服务端ToAll：\n" + msg + "\n");
            etSend.setText("");
        });

        findViewById(R.id.sendMsgOneBtn).setOnClickListener(view -> {
            final String msg = etSend.getText().toString();
            // 向服务器发送消息
            if (TextUtils.isEmpty(msg)) {
                return;
            }
            IpcServerHelper.getInstance().sendMsg("com.leo.socketclient", msg, true);
            msgTv.append("服务端To①：\n" + msg + "\n");
            etSend.setText("");
        });

        findViewById(R.id.sendMsgTwoBtn).setOnClickListener(view -> {
            final String msg = etSend.getText().toString();
            // 向服务器发送消息
            if (TextUtils.isEmpty(msg)) {
                return;
            }
            IpcServerHelper.getInstance().sendMsg("com.leo.socketclient2", msg, true);
            msgTv.append("服务端To②：\n" + msg + "\n");
            etSend.setText("");
        });
    }

    @Override
    protected void onDestroy() {
        IpcServerHelper.getInstance().finish(this);
        super.onDestroy();
    }
}