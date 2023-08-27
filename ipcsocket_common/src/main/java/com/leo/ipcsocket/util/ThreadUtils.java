package com.leo.ipcsocket.util;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {
    private static volatile ThreadUtils mInstance;
    private final ExecutorService executorService;

    private ThreadUtils() {
        executorService = new ThreadPoolExecutor(5, 10,
                30L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(512),
                new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    private static ThreadUtils getInstance() {
        if (mInstance == null) {
            synchronized (ThreadUtils.class) {
                if (mInstance == null) {
                    mInstance = new ThreadUtils();
                }
            }
        }
        return mInstance;
    }

    public static ExecutorService getExecutorService() {
        return getInstance().executorService;
    }
}
