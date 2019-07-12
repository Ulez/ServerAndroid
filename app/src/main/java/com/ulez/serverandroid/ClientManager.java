package com.ulez.serverandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

// 客户端的管理类
public class ClientManager {
    private static SharedPreferences sp;
    private static Context mContext;
    private static ServerStartCallBack callBack;
    private static NewMsgRecListener mNewMsgRecListener;
    private static Map<String, Socket> clientList = new HashMap<>();
    private static ServerThread serverThread = null;
    private static PrintWriter writer;

    public static class ServerThread implements Runnable {

        private int port = 45450;
        private boolean isExit = false;
        private ServerSocket server;
        private String ip;
        private Socket accept;

        public ServerThread() {
            try {
                server = new ServerSocket(port);
                ip = Util.getIPAddress(mContext);
                Log.e("lcy", "ip:" + ip + "启动服务成功" + "port:" + port);
                if (callBack != null) {
                    callBack.Success(ip + ":" + port, "启动服务成功");
                }
            } catch (IOException e) {
                if (callBack != null) {
                    callBack.Error("启动server失败，错误原因：" + e.getMessage());
                }
                Log.e("lcy", "启动server失败，错误原因：" + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                while (!isExit) {
                    // 进入等待环节
                    Log.e("lcy", "等待设备的连接... ... ");
                    accept = server.accept();
                    writer = new PrintWriter(accept.getOutputStream(), true);//告诉客户端连接成功 并传状态过去
                    // 获取手机连接的地址及端口号
                    final String address = accept.getRemoteSocketAddress().toString();
                    Log.i("lcy", "连接成功，连接的设备为：" + address);

                    InputStream inputStream = accept.getInputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        String data = new String(buffer, 0, len);
                        mNewMsgRecListener.onMsgRec(data);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void Stop() {
            isExit = true;
            if (server != null) {
                try {
                    server.close();
                    Log.e("lcy", "已关闭server");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean sendMsgToClient(String msg) {
            try {
//                String returnServer = "来自创维小度AI盒子：" + System.currentTimeMillis();
                if (writer.checkError()) return false;
                writer.print(msg);
                Log.e("lcy", "println服务器发送：" + msg);
                writer.flush();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static ServerThread startServer(Context contect, ServerStartCallBack serverStartCallBack, NewMsgRecListener newMsgRecListener) {
        callBack = serverStartCallBack;
        mContext = contect.getApplicationContext();
        mNewMsgRecListener = newMsgRecListener;
        Log.e("lcy", "开启服务");
        if (serverThread != null) {
            showDown();
        }
        serverThread = new ServerThread();
        new Thread(serverThread).start();
        Log.e("lcy", "开启服务成功");
        return serverThread;
    }

    // 关闭所有server socket 和 清空Map
    public static void showDown() {
        if (serverThread != null && clientList != null) {
            for (Socket socket : clientList.values()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (writer != null) {
                writer.close();
            }
            serverThread.Stop();
            clientList.clear();
        }
    }
}
