package com.fovoy.rest;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by zxz.zhang on 16/8/12.
 */
public interface RestHandler {

    void handle(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException,
            IOException;
}
