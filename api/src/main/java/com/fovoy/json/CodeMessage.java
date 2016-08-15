package com.fovoy.json;

/**
 * Created by zxz.zhang on 16/8/15.
 */
public interface CodeMessage {

    public static final int OK = 0;
    public static final int SYSTEM_ERROR = -1;

    int getStatus();

    String getMessage();

    Object getData();

}
