package com.fovoy.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by zxz.zhang on 16/8/14.
 */
public class ManagedThreadPool extends ThreadPoolExecutor implements TimeCounter {


    public ManagedThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                             BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, ManagedExecutors.defaultThreadFactory());
    }

    public ManagedThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                             BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, ManagedExecutors.defaultThreadFactory(), handler);
    }

    public ManagedThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                             BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public ManagedThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                             BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
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

    public static void main(String[] args) throws InterruptedException {

        ThreadPoolExecutor tpe = new ManagedThreadPool(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());

        tpe.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread.sleep(1000);
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            System.out.println(t);
        }

        tpe.shutdown();
    }
}
