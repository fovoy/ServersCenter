package com.fovoy.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

/**
 * Created by zxz.zhang on 16/8/13.
 */
class SwitchableContainer implements Switchable {
    private static final Logger logger = LoggerFactory.getLogger(SwitchableContainer.class);

    private final String key;
    private final Switchable switchable;
    private final BlockingQueue<HealthyChecker.Action> actions;

    private static final int INIT = 0;

    private static final int ONLINE = 1;

    private static final int OFFLINE = 2;

    private int state = INIT;

    public SwitchableContainer(String key, Switchable switchable, BlockingQueue<HealthyChecker.Action> actions) {
        this.key = key;
        this.switchable = switchable;
        this.actions = actions;
    }

    @Override
    public synchronized boolean offline() {
        if (state == OFFLINE) return false;
        try {
            boolean offline = switchable.offline();
            if (!offline) {
                logger.error("Service {} offline failed.", key);
                actions.offer(new HealthyChecker.Action(false, this));
                return false;
            }
            logger.info("set service {} offline success.", key);
        } catch (Throwable e) {
            logger.error("Service {} offline failed.", key, e);
            actions.offer(new HealthyChecker.Action(false, this));
            return false;
        }
        state = OFFLINE;
        return true;
    }

    @Override
    public synchronized boolean online() {
        if (state == ONLINE) return false;
        try {
            boolean online = switchable.online();
            if (!online) {
                logger.error("Service {} online failed.", key);
                actions.offer(new HealthyChecker.Action(true, this));
                return false;
            }
            logger.info("set service {} online success.", key);
        } catch (Throwable e) {
            logger.error("Service {} online failed.", key, e);
            actions.offer(new HealthyChecker.Action(true, this));
            return false;
        }
        state = ONLINE;
        return true;
    }
}
