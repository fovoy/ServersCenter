package com.fovoy.util;

import com.fovoy.json.CodeMessage;
import com.fovoy.json.JsonFeature;
import com.fovoy.json.JsonMapper;
import com.fovoy.json.MapperBuilder;
import com.fovoy.json.node.JacksonSupport;
import com.fovoy.json.node.MapNode;
import com.fovoy.json.node.ValueNode;
import com.google.common.base.Charsets;
import com.google.common.io.Closer;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import static com.fovoy.Constants.HEADER.*;
import static com.fovoy.Constants.Common.*;

/**
 * Created by zxz.zhang on 16/8/13.
 */
public class RequestUtil {

    public static final JsonMapper mapper = MapperBuilder.create().disable(JsonFeature.AUTO_CLOSE_TARGET).build();

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
    // POST JSON with Version
    public static ValueNode post(String remoteUrl, Object data) {

        OutputStreamWriter writer = null;
        InputStreamReader reader = null;
        Closer closer = Closer.create();
        String uri = remoteUrl.startsWith("http://") ? remoteUrl : "http://" + remoteUrl;

        try {
            URLConnection conn = new URL(uri).openConnection();
            conn.setRequestProperty("Content-Type", CONTENT_TYPE_JSON);
            conn.setRequestProperty(HEADER_VERSION, VERSION);
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(5000);

            writer = closer.register(new OutputStreamWriter(conn.getOutputStream(), Charsets.UTF_8));
            mapper.writeValue(writer, data);

            reader = closer.register(new InputStreamReader(conn.getInputStream(), Charsets.UTF_8));

            return JacksonSupport.parse(reader);
        } catch (Exception e) {

            return new MapNode().set("status", CodeMessage.SYSTEM_ERROR).set("message", "请求失败, " + uri);
        } finally {
            try {
                closer.close();
            } catch (IOException ignored) {

            }
        }
    }
}
