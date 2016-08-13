package com.fovoy.rest;

import com.fovoy.common.AbstractLifecycleComponent;
import com.fovoy.common.FovoyException;
import com.fovoy.util.RestUtils;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zxz.zhang on 16/8/12.
 */
public class RestController extends AbstractLifecycleComponent<RestController> {

    private final static Logger logger = LoggerFactory.getLogger(RestController.class);

    public static final String HTTP_JSON_ENABLE = "http.jsonp.enable";
    private ImmutableSet<String> relevantHeaders = ImmutableSet.of();
    private final String contextPath;
    private final PathTrie<RestHandler> handlers = new PathTrie<>(RestUtils.REST_DECODER);

    private final RestHandlerFilter handlerFilter = new RestHandlerFilter();


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

    public RestHandler getHandler(HttpServletRequest request) {
        String path = getPath(request);
        return handlers.retrieve(path, request.getParameterMap());
    }

    public boolean hasHandler(HttpServletRequest request) {
        String path = getPath(request);
        return handlers.retrieve(path, request.getParameterMap()) != null ? true : false;
    }

    public void dispatchRequest(final HttpServletRequest request, final HttpServletResponse response, final FilterChain channel) {

        if (filters.length == 0) {
            try {
                executeHandler(request, response, channel);
            } catch (Throwable e) {
                try {
                    channel.doFilter(request, response);
                } catch (Throwable e1) {
                    logger.error("failed to send failure response for uri [" + request.getRequestURI() + "]", e1);
                }
            }
        } else {
            ControllerFilterChain filterChain = new ControllerFilterChain(handlerFilter);
            filterChain.continueProcessing(request, response, channel);
        }
    }

    void executeHandler(HttpServletRequest request, HttpServletResponse response, FilterChain channel) throws Exception {
        final RestHandler handler = getHandler(request);
        if (handler != null) {
            handler.handle(request, response, channel);
        } else {
            channel.doFilter(request, response);
        }
    }

    private String getPath(HttpServletRequest request) {
        return request.getServletPath();
    }

    class ControllerFilterChain implements RestFilterChain {

        private final RestFilter executionFilter;

        private final AtomicInteger index = new AtomicInteger();

        ControllerFilterChain(RestFilter executionFilter) {
            this.executionFilter = executionFilter;
        }

        @Override
        public void continueProcessing(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) {
            try {
                int loc = index.getAndIncrement();
                if (loc > filters.length) {
                    throw new FovoyException("filter continueProcessing was called more than expected");
                } else if (loc == filters.length) {
                    executionFilter.process(request, response, chain, this);
                } else {
                    RestFilter preProcessor = filters[loc];
                    preProcessor.process(request, response, chain, this);
                }
            } catch (Exception e) {
                try {
                    chain.doFilter(request, response);
                } catch (Exception e1) {
                    logger.error("Failed to send failure response for uri [" + request.getRequestURI() + "]", e1);
                }
            }
        }
    }

    class RestHandlerFilter extends RestFilter {

        @Override
        public void process(HttpServletRequest request, HttpServletResponse response, FilterChain chain, RestFilterChain filterChain) throws Exception {
            executeHandler(request, response, chain);
        }
    }
}
