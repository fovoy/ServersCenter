package com.fovoy.rest;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by zxz.zhang on 16/8/13.
 */
public interface RestFilterChain {

    void continueProcessing(HttpServletRequest request,FilterChain chain);
}
