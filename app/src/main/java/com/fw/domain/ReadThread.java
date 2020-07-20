package com.fw.domain;



import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.Callable;

/**
 * @author yqf
 */
public class ReadThread implements Callable {


    private ObjectInputStream objectInputStream;


    public ReadThread(ObjectInputStream objectInputStream) {
        this.objectInputStream = objectInputStream;
    }


    @Override
    public Object call() throws Exception {
        Object msg = null;
        try{

            msg = objectInputStream.readObject();
            System.out.println(msg);

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("客户端连接异常"+e.getMessage());
        }
        return msg;
    }
}
