package com.fovoy.management;

import java.util.Map;

/**
 * Created by zxz.zhang on 16/8/12.
 */
public interface InfoReporter {
    String getName();

    Map<String, String> report();
}
