package com.leo.socketipc;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.leo.ipcsocket.server.IServerMsgCallback;
import com.leo.ipcsocket.server.IpcServerHelper;

public class MainActivity extends AppCompatActivity {

    private EditText etSend;
    private TextView msgTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IpcServerHelper.getInstance().addMsgCallback((pkgName, msg) -> {
            runOnUiThread(() -> msgTv.append("客户端：\n" + msg + "\n"));
        });
        IpcServerHelper.getInstance().init(this, true);

        msgTv = findViewById(R.id.msgTv);
        etSend = findViewById(R.id.etSend);
        Button sendMsgBtn = findViewById(R.id.sendMsgBtn);
        sendMsgBtn.setOnClickListener(view -> {
            final String msg = etSend.getText().toString();
            // 向服务器发送消息
            if (TextUtils.isEmpty(msg)) {
                return;
            }
            IpcServerHelper.getInstance().sendMsg(msg);
            msgTv.append("服务端：\n" + msg + "\n");
            etSend.setText("");
        });
    }

    @Override
    protected void onDestroy() {
        IpcServerHelper.getInstance().finish(this);
        super.onDestroy();
    }
}