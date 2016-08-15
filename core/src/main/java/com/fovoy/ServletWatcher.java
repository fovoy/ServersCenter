package com.fovoy;

import com.fovoy.handler.HealthCheckHandler;
import com.fovoy.handler.NoneHandler;
import com.fovoy.management.ServersManager;
import com.fovoy.rest.RestController;
import com.fovoy.rest.RestHandler;
import com.fovoy.server.JettyServer;
import com.fovoy.server.TomcatServer;
import com.fovoy.util.RequestUtil;
import com.google.common.base.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by zxz.zhang on 16/8/12.
 */
public class ServletWatcher implements ServletContextListener, Filter {

    private static Logger logger = LoggerFactory.getLogger(ServletWatcher.class);

    protected static ServersManager manager = ServersManager.getInstance();

    private static final AtomicBoolean initialized = new AtomicBoolean(false);


    public void init(FilterConfig config) throws ServletException {
        init(config.getServletContext());
        RestController rest = manager.getHandlers();
        rest.registerHandler("/healthcheck.html", new HealthCheckHandler(manager));

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
        manager.initRestController(context.getContextPath());
        setupHealthCheckPoller(context);
//        startInstrument();  // TODO: 16/8/13
        FovoyServer.ServerStart();
        fixPort(context);
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        try {
            // 内网才处理attach到ServerManager上的各个handler
            if (RequestUtil.isIntranet(request)) {
                if (manager.getHandlers().hasHandler(request)) {
                    manager.getHandlers().dispatchRequest(request, response, chain);
                    return;
                }
            }
        } catch (Throwable e) {
            logger.debug("process internal api error", e);
            return;
        }
        chain.doFilter(request, response);
        //trace // TODO: 16/8/13
    }

    private void setupHealthCheckPoller(final ServletContext context) {
        manager.setupHealthCheckPoller(new Supplier<String>() {
            @Override
            public String get() {
                return context.getRealPath("/healthcheck.html");
            }
        });
    }

    private void fixPort(ServletContext context) {
        Server server = manager.getConfig().getServer();
        if (server.getPort() <= 0) {
            server.setPort(portOf(context));
        }
    }

    private int portOf(ServletContext context) {
        try {
            String info = context.getServerInfo();
            if (info.startsWith("Apache Tomcat/")) {
                return new TomcatServer(context).getPort();
            }
            if (info.startsWith("jetty")) {
                return new JettyServer(context).getPort();
            }
            throw new RuntimeException("未知的服务器类型" + info);
        } catch (Throwable e) {
            logger.error("未能识别有效的服务器端口号，请在配置中心手动配置。如server.port=8080", e);
            return 0;
        }
    }


    public void destroy() {
        if (initialized.compareAndSet(true, false))
            FovoyServer.ServerStop();
    }

}
