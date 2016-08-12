package com.fovoy;

import com.fovoy.management.ServersManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by zxz.zhang on 16/8/12.
 */
public class ServletWatcher implements ServletContextListener, Filter {

    private static Logger logger = LoggerFactory.getLogger(ServletWatcher.class);

    protected static ServersManager manager = ServersManager.getInstance();

    private static final AtomicBoolean initialized = new AtomicBoolean(false);


    public void init(FilterConfig filterConfig) throws ServletException {

    }


    public void contextInitialized(ServletContextEvent sce) {
        init(sce.getServletContext());
    }

    public void contextDestroyed(ServletContextEvent sce) {
        destroy();
    }

    private void init(ServletContext context) {
        if (!initialized.compareAndSet(false, true))
            return;
//        manager.initHandlerContainer(context.getContextPath());
//        setupHealthCheckPoller(context);
//        startInstrument();
//        fixPort(context);
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

    }


    public void destroy() {

    }

}
