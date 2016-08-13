package com.fovoy.util;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import java.io.File;
import java.io.IOException;

/**
 * Created by zxz.zhang on 16/8/13.
 */
public class FovoyFileUtil {
    private static Supplier<String> store = Suppliers.memoize(() -> {
        String path = System.getProperty("fovoy.cache", null);

        if (path == null) {
            path = System.getProperty("catalina.base");
            if (path == null) path = System.getProperty("java.io.tmpdir");
            path = path + File.separator + "cache";
            System.setProperty("fovoy.cache", path);
        }

        File file = new File(path);
        file.mkdirs();

        try {
            path = file.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return path;
    });

    public static String getFovoyStore() {
        return store.get();
    }
}
