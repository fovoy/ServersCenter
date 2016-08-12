package com.fovoy.common;

/**
 * Created by zxz.zhang on 16/8/12.
 */
public interface LifecycleComponent<T> extends Releasable {
    Lifecycle.State lifecycleState();

    void addLifecycleListener(LifecycleListener listener);

    void removeLifecycleListener(LifecycleListener listener);

    T start() throws FovoyException;

    T stop() throws FovoyException;
}
