package com.fovoy.management;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by zxz.zhang on 16/8/12.
 */
public interface FovoyServerManagement {
    @Deprecated
    String getConfigPath();

    String getFovoyStore();

    ThreadPoolExecutor getExecutor(String name);

    <T> T getService(String name);

    <T> T getResource(String name);
}
