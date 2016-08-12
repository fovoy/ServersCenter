package com.fovoy;

import com.fovoy.management.BlockingManager;
import com.fovoy.management.FovoyServerManagement;
import com.fovoy.management.ServerManagement;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zxz.zhang on 16/8/12.
 */
public class ServersContainer<T> {

    private static final Map<Class, String> errorMessages = new HashMap<Class, String>();

    static {
        errorMessages.put(ServerManagement.class, "请检查是否引用了core，并且各个包版本必须一致");
        errorMessages.put(FovoyServerManagement.class, "请检查是否引用了core，并且各个包版本必须一致");
        errorMessages.put(BlockingManager.class, "请检查是否引用了blocking，并且各个包版本必须一致");
    }

    private static final ConcurrentHashMap<Class<?>, ServersContainer<?>> instanceMap = new ConcurrentHashMap<Class<?>, ServersContainer<?>>();

    private final Supplier<T> supplier;

    private ServersContainer(final Class<T> clz) {
        supplier = Suppliers.memoize(() -> {
            ServiceLoader<T> loader = ServiceLoader.load(clz);
            return Iterables.getFirst(loader, null);
        });
    }


    public static <T> T getService(Class<T> clz) {
        T instance = getServiceWithoutCheck(clz);
        Preconditions.checkNotNull(instance, errorMessages.get(clz));
        return instance;
    }

    public static <T> T getServiceWithoutCheck(Class<T> clz) {
        Preconditions.checkNotNull(clz);
        Preconditions.checkArgument(clz.isInterface(), "clz is not a interface");
        ServersContainer<T> serversContainer = (ServersContainer<T>) instanceMap.get(clz);
        if (serversContainer == null) {
            instanceMap.putIfAbsent(clz, new ServersContainer<T>(clz));
            serversContainer = (ServersContainer<T>) instanceMap.get(clz);
        }
        return serversContainer.supplier.get();
    }

    public static void main(String[] args) {
        ServersContainer s=new ServersContainer<Test>(Test.class);
        System.out.println(s.supplier.get());
        Preconditions.checkNotNull(s.supplier.get(),"supplier.get() 为空");
    }
}
