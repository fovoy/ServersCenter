package com.fovoy.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zxz.zhang on 16/8/14.
 */
public class FovoyThreadFactory implements ThreadFactory {


    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final boolean mDaemo;

    public FovoyThreadFactory(String namePrefix) {
        this(namePrefix, true);
    }

    public FovoyThreadFactory(String namePrefix, boolean mDaemo) {
        this.mDaemo = mDaemo;
        this.namePrefix = namePrefix;
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, namePrefix + "[T#" + threadNumber.getAndIncrement() + "]", 0);
        t.setDaemon(mDaemo);
        return t;
    }
}
