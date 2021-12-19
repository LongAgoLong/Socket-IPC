package com.leo.socketipc;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.leo.ipcsocket.server.IpcServerHelper;
import com.leo.ipcsocket.server.IpcSocketService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IpcServerHelper.getInstance().init(this, true);

        Button sendMsgBtn = findViewById(R.id.sendMsgBtn);
        sendMsgBtn.setOnClickListener(view -> {
            IpcServerHelper.getInstance().sendMsg("");
        });
    }

    @Override
    protected void onDestroy() {
        IpcServerHelper.getInstance().finish(this);
        super.onDestroy();
    }
}