package com.ulez.serverandroid;

public interface ServerStartCallBack {
    void Success(String ip,String status);

    void Error(String msg);
}
