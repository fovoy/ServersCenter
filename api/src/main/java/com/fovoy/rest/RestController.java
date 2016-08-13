package com.fovoy.rest;

import com.fovoy.common.AbstractLifecycleComponent;
import com.fovoy.common.FovoyException;
import com.fovoy.util.RestUtils;
import com.google.common.collect.ImmutableSet;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by zxz.zhang on 16/8/12.
 */
public class RestController extends AbstractLifecycleComponent<RestController> {

    public static final String HTTP_JSON_ENABLE = "http.jsonp.enable";
    private ImmutableSet<String> relevantHeaders = ImmutableSet.of();
    private final String contextPath;
    private final PathTrie<RestHandler> handlers = new PathTrie<>(RestUtils.REST_DECODER);


    private RestFilter[] filters = new RestFilter[0];


    public RestController(String contextPath) {
        this.contextPath = contextPath;
    }

    public RestController() {
        this.contextPath = "";
    }
    @Override
    protected void doStart() throws FovoyException {

    }

    @Override
    protected void doStop() throws FovoyException {

    }

    @Override
    public void close() throws FovoyException {
        super.close();
    }

    @Override
    protected void doClose() throws FovoyException {
        for (RestFilter filter : filters) {
            filter.close();
        }
    }

    public synchronized void registerFilter(RestFilter preProcessor) {
        RestFilter[] copy = new RestFilter[filters.length + 1];
        System.arraycopy(filters, 0, copy, 0, filters.length);
        copy[filters.length] = preProcessor;
        Arrays.sort(copy, (o1, o2) -> Integer.compare(o1.order(), o2.order()));
        filters = copy;
    }

    public void registerHandler(String path, RestHandler handler) {
        handlers.insert(path, handler);
    }

    private RestHandler getHandler(HttpServletRequest request) {
        String path = getPath(request);
        return handlers.retrieve(path, request.getParameterMap());
    }

    private String getPath(HttpServletRequest request) {
        return request.getServletPath();
    }
}
