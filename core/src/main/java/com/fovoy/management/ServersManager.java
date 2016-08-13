package com.fovoy.management;

import com.fovoy.ServerConfig;
import com.fovoy.ServersContainer;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by zxz.zhang on 16/8/12.
 */
public class ServersManager {
    private static Logger logger = LoggerFactory.getLogger(ServersManager.class);

    private AtomicBoolean isSetupHealthCheckPoller = new AtomicBoolean(false);

    private static Supplier<ServerManagement> holder = Suppliers.memoize(() -> ServersContainer.getService(ServerManagement.class));

    private static AtomicReference<ServersManager> instance = new AtomicReference<ServersManager>();

    private final ServerConfig serverconfig;

    public static ServersManager getInstance() {
        holder.get();
        return instance.get();
    }

    private final AtomicReference<HandlerContainer> handlers = new AtomicReference<HandlerContainer>();

    private final ConcurrentMap<String, InfoReporter> reporters = Maps.newConcurrentMap();

    public ServersManager() {
        this.serverconfig = initAppConfig();
    }

    private static ServerConfig initAppConfig() {
      return null;//// TODO: 16/8/13
    }
    public synchronized void setupHealthCheckPoller(final Supplier<String> pathGetter) {
        if (isSetupHealthCheckPoller.get()) return;

        isSetupHealthCheckPoller.compareAndSet(false,true);
//        HEALTHY_CHECKER.start(pathGetter);  // TODO: 16/8/13
    }

    public ServerConfig getServerconfig() {
        return serverconfig;
    }
}
