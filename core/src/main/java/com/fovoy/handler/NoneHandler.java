package com.fovoy.handler;

import com.fovoy.rest.RestHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Created by zxz.zhang on 16/8/13.
 */
public class NoneHandler implements RestHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        chain.doFilter(request, response);
    }
}
