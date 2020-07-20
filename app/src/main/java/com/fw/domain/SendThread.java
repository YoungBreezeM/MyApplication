package com.fw.domain;

import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * @author yqf
 */
public class SendThread implements Runnable {

    private ObjectOutputStream objectOutputStream;
    private Body body;

    public SendThread(ObjectOutputStream objectOutputStream, Body body) {
        this.objectOutputStream = objectOutputStream;
        this.body = body;
    }

    @Override
    public void run() {
        try {
                objectOutputStream.writeObject(JSON.toJSONString(body));
                objectOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
