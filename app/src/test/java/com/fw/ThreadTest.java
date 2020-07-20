package com.fw;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ThreadTest {
    public static void main(String[] args) {
        ScheduledExecutorService service = Executors
                .newSingleThreadScheduledExecutor();

        ScheduledFuture<?> schedule = service.schedule(new Runnable() {
            @Override
            public void run() {
                int num = 0;
                while (num < 100) {
                    num++;
                    System.out.println(num);
                }
            }
        }, 1000, TimeUnit.MILLISECONDS);
    }
}
