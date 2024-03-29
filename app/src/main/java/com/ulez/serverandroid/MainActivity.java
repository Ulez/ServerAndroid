package com.ulez.serverandroid;

import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int SEND_SUCCESS = 1;
    private static final int CONNECT_SUCCESS = 4;
    private static final int SEND_ERROR = 2;
    private static final int RECIEVE_SUCCESS = 3;
    private TextView tvStatus;
    private EditText etIp;
    private ListView listView;
    private ArrayAdapter adapter;
    private ArrayList adapterData;
    private EditText etStr;
    private Button btSend;
    private ClientManager.ServerThread serverThread;
    private MyHandler myHandler;

    private static class MyHandler extends WeakHandler<MainActivity> {

        public MyHandler(MainActivity context) {
            super(context);
        }

        @Override
        public void handle(MainActivity activity, Message msg) {
            switch (msg.what) {
                case CONNECT_SUCCESS:
                    activity.adapterData.add("client:" + msg.obj);
                    activity.adapter.notifyDataSetChanged();
                    break;
                case SEND_SUCCESS:
                    activity.adapterData.add("发送:" + msg.obj);
                    activity.adapter.notifyDataSetChanged();
                    break;
                case SEND_ERROR:
                    Toast.makeText(activity, "发送失败:" + msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                case RECIEVE_SUCCESS:
                    activity.adapterData.add("收到:" + msg.obj);
                    activity.adapter.notifyDataSetChanged();
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myHandler = new MyHandler(this);
        tvStatus = findViewById(R.id.tv_status);
        etIp = findViewById(R.id.et_ip);
        listView = findViewById(R.id.lv);
        etStr = findViewById(R.id.et_str);
        btSend = findViewById(R.id.bt);
        btSend.setOnClickListener(this);
        adapterData = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, adapterData);
        listView.setAdapter(adapter);

        serverThread = ClientManager.startServer(this, new ServerStartCallBack() {
            @Override
            public void Success(String ip, String status) {
                tvStatus.setText(status);
                etIp.setText(ip);
            }

            @Override
            public void Error(String msg) {
                tvStatus.setText(msg);
            }
        }, new NewMsgRecListener() {
            @Override
            public void onMsgRec(String msg) {
                myHandler.obtainMessage(RECIEVE_SUCCESS, msg).sendToTarget();
            }

            @Override
            public void onConnectState(String status) {
                myHandler.obtainMessage(CONNECT_SUCCESS, status).sendToTarget();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt:
                final String msg = etStr.getText().toString();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (serverThread.sendMsgToClient(msg)) {
                            myHandler.obtainMessage(SEND_SUCCESS, msg).sendToTarget();
                        } else {
                            myHandler.obtainMessage(SEND_ERROR, msg).sendToTarget();
                        }
                    }
                }).start();
                break;
        }

    }
}
