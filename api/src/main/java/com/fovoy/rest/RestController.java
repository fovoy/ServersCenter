package com.fovoy.rest;

import com.fovoy.common.AbstractLifecycleComponent;
import com.fovoy.common.FovoyException;
import com.fovoy.util.RestUtils;
import com.google.common.collect.ImmutableSet;

/**
 * Created by zxz.zhang on 16/8/12.
 */
public class RestController extends AbstractLifecycleComponent<RestController> {

    public static final String HTTP_JSON_ENABLE = "http.jsonp.enable";
    private ImmutableSet<String> relevantHeaders = ImmutableSet.of();

    private final PathTrie<RestHandler> getHandlers = new PathTrie<>(RestUtils.REST_DECODER);
    private final PathTrie<RestHandler> postHandlers = new PathTrie<>(RestUtils.REST_DECODER);
    private final PathTrie<RestHandler> putHandlers = new PathTrie<>(RestUtils.REST_DECODER);
    private final PathTrie<RestHandler> deleteHandlers = new PathTrie<>(RestUtils.REST_DECODER);
    private final PathTrie<RestHandler> headHandlers = new PathTrie<>(RestUtils.REST_DECODER);
    private final PathTrie<RestHandler> optionsHandlers = new PathTrie<>(RestUtils.REST_DECODER);



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

    }
}
