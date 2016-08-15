package com.fovoy.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by zxz.zhang on 16/8/15.
 */
public abstract class TimeTask implements Runnable {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean run = new AtomicBoolean();

    protected final Executor exector;

    public TimeTask() {
        this(ManagedExecutors.getExecutor());
    }

    public TimeTask(Executor e) {
        this.exector = e;
    }

    @Override
    public void run() {

        try {
            while (run.compareAndSet(true, false)) {
                try {
                    runTask();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            running.set(false);
        }
    }

    public boolean weakUp() {
        run.set(true);
        if (running.compareAndSet(false, true)) {
            exector.execute(this);
            return true;
        }
        return false;
    }


    protected abstract void runTask();
}
