package com.fovoy;

import javax.servlet.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by zxz.zhang on 16/8/12.
 */
public class ServletWatcher implements ServletContextListener ,Filter {

    private static final AtomicBoolean initialized = new AtomicBoolean(false);


    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

    }


    public void destroy() {

    }

    public void contextInitialized(ServletContextEvent sce) {

    }
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
