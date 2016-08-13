package com.fovoy.rest;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by zxz.zhang on 16/8/13.
 */
public interface RestFilterChain {

    void continueProcessing(final HttpServletRequest request, final HttpServletResponse response, final FilterChain channel);
}
