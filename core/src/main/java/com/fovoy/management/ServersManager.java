package com.fovoy.management;

import com.fovoy.ServersContainer;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by zxz.zhang on 16/8/12.
 */
public class ServersManager {
    private static Logger logger = LoggerFactory.getLogger(ServersManager.class);

    private static Supplier<ServerManagement> holder = Suppliers.memoize(() -> ServersContainer.getService(ServerManagement.class));

    private static AtomicReference<ServersManager> instance = new AtomicReference<ServersManager>();
    public static ServersManager getInstance() {
        holder.get();
        return instance.get();
    }

    private final AtomicReference<HandlerContainer> handlers = new AtomicReference<HandlerContainer>();

    private final ConcurrentMap<String, InfoReporter> reporters = Maps.newConcurrentMap();
}
