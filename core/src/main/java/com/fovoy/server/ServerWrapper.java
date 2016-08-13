package com.fovoy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;

/**
 * 应用服务器包装类.读取服务器信息
 * Created by zxz.zhang on 16/8/13.
 */
public abstract class ServerWrapper {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected ServletContext context;

    /** 应用服务端口 */
    protected int port;

    public ServerWrapper(ServletContext context) {
        this.context = context;
        extractContext(context);
    }

    public ServletContext getServletContext() {
        return context;
    }

    public int getPort() {
        return port;
    }

    protected abstract void extractContext(ServletContext context);
}
