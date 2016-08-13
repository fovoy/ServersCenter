package com.fovoy.util;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static com.fovoy.Constants.HEADER.*;

/**
 * Created by zxz.zhang on 16/8/13.
 */
public class RequestUtil {



    /**
     * 是否内网请求
     *
     * @param request
     * @return
     */
    public static boolean isIntranet(HttpServletRequest request) {
        String xRealIp = request.getHeader(HEADER_X_REAL_IP);
        //直接访问，一般是内网访问
        if (xRealIp == null || xRealIp.length() == 0) return true;
        try {
            InetAddress address = InetAddress.getByName(xRealIp);
            return address.isSiteLocalAddress() || address.isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
