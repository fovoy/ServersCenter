package com.fovoy.management;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by zxz.zhang on 16/8/13.
 */
public class HealthyChecker {
    private static final Logger logger = LoggerFactory.getLogger(HealthyChecker.class);

    private final ConcurrentMap<String, Switchable> switchers = Maps.newConcurrentMap();
    private final ConcurrentMap<String, Switchable> switchersToAdd = Maps.newConcurrentMap();
    private final ConcurrentMap<String, HealthChecker> checkers = Maps.newConcurrentMap();

    private final BlockingQueue<Action> failed = new LinkedBlockingQueue<Action>();

    // 应用上下线调用状态，实现幂等，避免重复调用
    private AtomicBoolean state = new AtomicBoolean(true);

    // 系统所有组件是否已经全部上线
    private volatile boolean isOnline = false;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final Lock readLock = readWriteLock.readLock();

    private final Lock writeLock = readWriteLock.writeLock();


    public void start(final Supplier<String> pathGetter) {
        state.set(false);
        logger.info("启用上下线机制，上下线控制文件路径: {}", pathGetter.get());

//        ManagedExecutors.getScheduleExecutor().scheduleAtFixedRate(new Runnable() {  //// TODO: 16/8/13
//            @Override
//            public void run() {
//                String name = Thread.currentThread().getName();
//                try {
//                    Thread.currentThread().setName("common-healthchecker");
//                    String path = pathGetter.get();
//                    boolean resultState = checkFile(path) && checkAll();
//
//                    boolean changed;
//                    writeLock.lock();
//                    try {
//                        changed = state.compareAndSet(!resultState, resultState);
//                        switchers.putAll(switchersToAdd);
//                        switchersToAdd.clear();
//                    } finally {
//                        writeLock.unlock();
//                    }
//
//                    if (resultState) {
//                        onlineAll(changed);
//                    } else {
//                        offlineAll(changed);
//                    }
//                } finally {
//                    Thread.currentThread().setName(name);
//                }
//            }
//        }, 5, 2, TimeUnit.SECONDS);
    }

    public void addSwitcher(String key, Switchable switchable) {
        SwitchableContainer container = new SwitchableContainer(key, switchable, failed);
        readLock.lock();
        try {
            if (switchers.get(key) != null || switchersToAdd.putIfAbsent(key, container) != null) {
                logger.warn("OfflineListener ({}) already exists", key, new RuntimeException());
                return;
            }

            logger.info("OfflineListener ({}) added", key);
            if (state.get()) {
                container.online();
            } else {
                container.offline();
            }
        } finally {
            readLock.unlock();
        }
    }

    public void addHealthChecker(String key, HealthChecker checker) {
        if (checkers.putIfAbsent(key, checker) != null) {
            logger.warn("HealthChecker ({}) already exists", key, new RuntimeException());
        } else {
            logger.info("HealthChecker ({}) added", key);
        }
    }

    public Map<String, HealthChecker> getAllCheckers() {
        return Collections.unmodifiableMap(checkers);
    }

    public boolean checkAll() {
        for (Map.Entry<String, HealthChecker> entry : checkers.entrySet()) {
            String key = entry.getKey();
            try {
                if (!entry.getValue().check()) {
                    logger.warn("HealthChecker ({}) failed!", key);
                    return false;
                }
            } catch (Throwable t) {
                logger.warn("HealthChecker ({}) failed!", key, t);
                return false;
            }
        }

        return true;
    }

    public boolean healthCheck() {
        return isOnline && checkAll();
    }

    private void offlineAll(boolean changed) {
        isOnline = false;
        recover(false);

        if (changed) {
            Map<String, Switchable> copy = new HashMap<String, Switchable>(switchers);
            for (Map.Entry<String, Switchable> entry : copy.entrySet()) {
                Switchable switchable = entry.getValue();
                switchable.offline();
            }
        }
    }

    private void onlineAll(boolean changed) {
        recover(true);
        if (changed) {
            Map<String, Switchable> copy = new HashMap<String, Switchable>(switchers);
            for (Map.Entry<String, Switchable> entry : copy.entrySet()) {
                Switchable switchable = entry.getValue();
                switchable.online();
            }
            isOnline = true;
        }
    }

    private void recover(boolean type) {
        List<Action> actions = Lists.newArrayListWithCapacity(failed.size());
        failed.drainTo(actions);
        for (Action action : actions) {
            action.action(type);
        }
    }

    private boolean checkFile(String path) {
        if (StringUtils.isEmpty(path)) {
            return false;
        }
        return new File(path).exists();
    }

    static class Action {
        private final boolean type;
        private final Switchable switchable;

        public Action(boolean type, Switchable switchable) {
            this.type = type;
            this.switchable = switchable;
        }

        public void action(boolean expected) {
            if (this.type != expected) return;
            if (this.type) {
                switchable.online();
            } else {
                switchable.offline();
            }
        }

    }
}
