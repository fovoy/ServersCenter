package com.fovoy.common;

/**
 * Created by zxz.zhang on 16/8/12.
 */
public class FovoyException extends RuntimeException {

    public FovoyException(String msg) {
        super(msg);
    }

    public FovoyException(String message, Throwable cause) {
        super(message, cause);
    }
}
