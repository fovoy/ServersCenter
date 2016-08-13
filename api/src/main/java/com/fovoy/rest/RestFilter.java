package com.fovoy.rest;

import com.fovoy.common.FovoyException;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Closeable;

/**
 * Created by zxz.zhang on 16/8/13.
 */
public abstract class RestFilter implements Closeable {

    public int order() {
        return 0;
    }

    @Override
    public void close() throws FovoyException {
        // a no op
    }


    public abstract void process(HttpServletRequest request, HttpServletResponse response, FilterChain chain, RestFilterChain filterChain) throws Exception;

}
