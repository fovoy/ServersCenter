package com.fovoy.server;

import javax.servlet.ServletContext;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context.SContext;

/**
 * Created by zxz.zhang on 16/8/13.
 */
public class JettyServer extends ServerWrapper {

    public JettyServer(ServletContext context) {
        super(context);
    }

    @Override
    protected void extractContext(ServletContext _context) {
        SContext context = (SContext) _context;
        Server server = context.getContextHandler().getServer();
        Connector connector = server.getConnectors()[0];

        port = connector.getPort();
    }
}
