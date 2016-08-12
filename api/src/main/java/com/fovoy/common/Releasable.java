package com.fovoy.common;

/**
 * Created by zxz.zhang on 16/8/12.
 */
public interface Releasable extends AutoCloseable {

    void close() throws FovoyException;
}
