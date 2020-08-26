package com.yale.rpc.handler;

public class SendRequestThread extends Thread {

    private Class<?> clazz;

    private UserClientHandler userClientHandler;

    public SendRequestThread(Class<?> clazz,UserClientHandler userClientHandler){
        this.clazz=clazz;
        this.userClientHandler=userClientHandler;
    }

    @Override
    public void run() {

        super.run();
    }
}
