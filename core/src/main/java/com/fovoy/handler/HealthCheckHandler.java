package com.fovoy.handler;

import com.fovoy.management.ServersManager;
import com.fovoy.rest.RestHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by zxz.zhang on 16/8/13.
 */
public class HealthCheckHandler implements RestHandler {

    private ServersManager manager;

    public HealthCheckHandler(ServersManager manager) {
        this.manager = manager;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (!manager.healthCheck()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } else {
            chain.doFilter(request, response);
        }
    }
}
