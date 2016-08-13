package com.fovoy.util;

/**
 * Created by zxz.zhang on 16/8/13.
 */
public class LogUtil {
    public static void log() {
        System.out.println("***************************************************");
    }

    public static void log(Object... args) {
        StringBuilder builder = new StringBuilder("* ");

        for (Object obj : args) {
            builder.append(obj);
        }

        System.out.println(builder);
    }
}
