package com.fovoy.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by zxz.zhang on 16/8/14.
 */
public class ManagedScheduledThreadPool extends ScheduledThreadPoolExecutor implements TimeCounter {

    public ManagedScheduledThreadPool(int corePoolSize) {
        super(corePoolSize, ManagedExecutors.defaultThreadFactory());
    }

    public ManagedScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    public ManagedScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
    }

    public ManagedScheduledThreadPool(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, ManagedExecutors.defaultThreadFactory(), handler);
    }


    private static final Logger log = LoggerFactory.getLogger(ManagedThreadPool.class);

    private final AtomicLong finishTime = new AtomicLong();

    private final static ThreadLocal<Long> local = new ThreadLocal<Long>();

    public long getFinishTime() {
        return finishTime.get();
    }

    @Override
    public void beforeExecute(Thread t, Runnable r) {
        local.set(System.currentTimeMillis());
        super.beforeExecute(t, r);
        try {
            ThreadRecycles.init();
        } catch (RuntimeException e) {
            log.warn("ThreadRecycles.init error", e);
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable ex) {
        try {
            long time = local.get();
            local.remove();
            finishTime.addAndGet(System.currentTimeMillis() - time);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            ThreadRecycles.release();
        } catch (Throwable e) {
            log.warn("ThreadRecycles.release error", e);
        } finally {
            super.afterExecute(r, ex);
        }

        if (ex != null)
            log.warn("在线程池中捕获到未知异常:" + ex, ex);
    }


}
