package com.fovoy.management;

import com.fovoy.ServerConfig;
import com.fovoy.ServersContainer;
import com.fovoy.rest.RestController;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
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

    private static AtomicReference<ServersManager> instance = new AtomicReference();

    private static final HealthyChecker HEALTHY_CHECKER = new HealthyChecker();

    private final ServerConfig serverconfig;

    public static ServersManager getInstance() {
        holder.get();
        return instance.get();
    }

    private final AtomicReference<RestController> handlers = new AtomicReference();

    private final ConcurrentMap<String, InfoReporter> reporters = Maps.newConcurrentMap();

    public ServersManager() {
        this.serverconfig = initAppConfig();
    }

    private static ServerConfig initAppConfig() {
        return null;//// TODO: 16/8/13
    }

    public synchronized void setupHealthCheckPoller(final Supplier<String> pathGetter) {
        if (isSetupHealthCheckPoller.get()) return;

        isSetupHealthCheckPoller.compareAndSet(false, true);
//        HEALTHY_CHECKER.start(pathGetter);  // TODO: 16/8/13
    }

    public void initRestController(final String contextPath) {
        handlers.compareAndSet(null, new RestController(contextPath));
    }

    public RestController getHandlers() {
        // 防止非tomcat项目中handlers未初始化时直接调用getHandlers().addRequestHandler出错。
        handlers.compareAndSet(null, new RestController());
        return handlers.get();
    }
    public boolean healthCheck() {
        return HEALTHY_CHECKER.healthCheck();
    }

    public ServerConfig getServerconfig() {
        return serverconfig;
    }
}
