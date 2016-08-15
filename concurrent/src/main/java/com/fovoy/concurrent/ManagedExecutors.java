package com.fovoy.concurrent;

import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by zxz.zhang on 16/8/14.
 */
public class ManagedExecutors {
    private ManagedExecutors() {
    }

    private static final ThreadFactory cachedFactory = new FovoyThreadFactory("shared-pool");
    private static final ThreadFactory scheduleFactory = new FovoyThreadFactory("shared-sched");

    private static final ManagedThreadPool executor = (ManagedThreadPool) newCachedThreadPool();
    private static final ManagedScheduledThreadPool mstp = (ManagedScheduledThreadPool) newScheduledThreadPool(10);

    static {
        executor.setThreadFactory(cachedFactory);
        mstp.setThreadFactory(scheduleFactory);
    }

    public static ManagedThreadPool getExecutor() {
        return executor;
    }

    public static ManagedScheduledThreadPool getScheduleExecutor() {
        return mstp;
    }

    public static ExecutorService newFixedThreadPool(int nThreads) {
        return new ManagedThreadPool(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public static ExecutorService newFixedThreadPool(int nThreads, String threadName) {
        return new ManagedThreadPool(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new FovoyThreadFactory(threadName));
    }

    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return new ManagedThreadPool(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), threadFactory);
    }

    public static ExecutorService newSingleThreadExecutor() {
        return new FinalizableDelegatedExecutorService(new ManagedThreadPool(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>()));
    }

    public static ExecutorService newSingleThreadExecutor(String threadName) {
        return new FinalizableDelegatedExecutorService(new ManagedThreadPool(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new FovoyThreadFactory(threadName)));
    }

    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        return new FinalizableDelegatedExecutorService(new ManagedThreadPool(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), threadFactory));
    }

    public static ExecutorService newCachedThreadPool() {
        return new ManagedThreadPool(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    }

    public static ExecutorService newCachedThreadPool(String threadName) {
        return new ManagedThreadPool(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                new FovoyThreadFactory(threadName));
    }

    public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        return new ManagedThreadPool(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                threadFactory);
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return Executors.unconfigurableScheduledExecutorService(new ManagedScheduledThreadPool(1));
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String threadName) {
        return Executors.unconfigurableScheduledExecutorService(new ManagedScheduledThreadPool(1,
                new FovoyThreadFactory(threadName)));
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        return Executors.unconfigurableScheduledExecutorService(new ManagedScheduledThreadPool(1, threadFactory));
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
        return new ManagedScheduledThreadPool(corePoolSize);
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, String threadName) {
        return new ManagedScheduledThreadPool(corePoolSize, new FovoyThreadFactory(threadName));
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
        return new ManagedScheduledThreadPool(corePoolSize, threadFactory);
    }

    public static ExecutorService unconfigurableExecutorService(ExecutorService executor) {
        return Executors.unconfigurableExecutorService(executor);
    }

    public static ScheduledExecutorService unconfigurableScheduledExecutorService(ScheduledExecutorService executor) {
        return Executors.unconfigurableScheduledExecutorService(executor);
    }

    private static final ThreadFactory NON_FAC = new FovoyThreadFactory("anon-pool");

    public static ThreadFactory defaultThreadFactory() {
        String name = null;
        Throwable t = new Throwable();
        for (StackTraceElement st : t.getStackTrace()) {
            String className = st.getClassName();
            if (!className.startsWith("com.fovoy.concurrent")) {
                name = "anon-" + st.getClassName() + "." + st.getMethodName() + ":" + st.getLineNumber();
                return new FovoyThreadFactory(name);
            }
        }
        return NON_FAC;
    }

    public static ThreadFactory privilegedThreadFactory() {
        return Executors.privilegedThreadFactory();
    }

    public static <T> Callable<T> callable(Runnable task, T result) {
        return Executors.callable(task, result);
    }

    public static Callable<Object> callable(Runnable task) {
        return Executors.callable(task);
    }

    public static Callable<Object> callable(final PrivilegedAction<?> action) {
        return Executors.callable(action);
    }

    public static Callable<Object> callable(final PrivilegedExceptionAction<?> action) {
        return Executors.callable(action);
    }

    public static <T> Callable<T> privilegedCallable(Callable<T> callable) {
        return Executors.privilegedCallable(callable);
    }

    public static <T> Callable<T> privilegedCallableUsingCurrentClassLoader(Callable<T> callable) {
        return Executors.privilegedCallableUsingCurrentClassLoader(callable);
    }

    /**
     * A wrapper class that exposes only the ExecutorService methods of an ExecutorService implementation.
     */
    static class DelegatedExecutorService extends AbstractExecutorService {
        private final ExecutorService e;

        DelegatedExecutorService(ExecutorService executor) {
            e = executor;
        }

        public void execute(Runnable command) {
            e.execute(command);
        }

        public void shutdown() {
            e.shutdown();
        }

        public List<Runnable> shutdownNow() {
            return e.shutdownNow();
        }

        public boolean isShutdown() {
            return e.isShutdown();
        }

        public boolean isTerminated() {
            return e.isTerminated();
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return e.awaitTermination(timeout, unit);
        }

        public Future<?> submit(Runnable task) {
            return e.submit(task);
        }

        public <T> Future<T> submit(Callable<T> task) {
            return e.submit(task);
        }

        public <T> Future<T> submit(Runnable task, T result) {
            return e.submit(task, result);
        }

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return e.invokeAll(tasks);
        }

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {
            return e.invokeAll(tasks, timeout, unit);
        }

        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return e.invokeAny(tasks);
        }

        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return e.invokeAny(tasks, timeout, unit);
        }
    }

    static class FinalizableDelegatedExecutorService extends DelegatedExecutorService {
        FinalizableDelegatedExecutorService(ExecutorService executor) {
            super(executor);
        }
        protected void finalize() {
            super.shutdown();
        }
    }
}
