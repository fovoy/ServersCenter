package com.fovoy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;

import javax.servlet.ServletException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by zxz.zhang on 16/8/13.
 */
public class Server implements Serializable {


    private static final long serialVersionUID = 4653216035763825854L;

    private final String app;
    private final int pid;
    private final Type type;
    private final String hostname;
    private final String room;
    private final String ip;
    int port;
    private final String logdir;
    private final String token;

    @JsonCreator
    public Server(@JsonProperty("app") String app,
                     @JsonProperty("pid") int pid,
                     @JsonProperty("type") Type type,
                     @JsonProperty("hostname") String hostname,
                     @JsonProperty("room") String room,
                     @JsonProperty("ip") String ip,
                     @JsonProperty("port") int port,
                     @JsonProperty("logdir") String logdir,
                     @JsonProperty("token") String token) {

        this.app = app;
        this.pid = pid;
        this.type = type;
        this.hostname = hostname;
        this.room = room;
        this.ip = ip;
        this.port = port;
        this.logdir = logdir;
        this.token = token;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getApp() {
        return app;
    }

    public int getPid() {
        return pid;
    }

    public Type getType() {
        return type;
    }

    public String getHostname() {
        return hostname == null ? ip : hostname;
    }

    public String getRoom() {
        return room;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getToken() {
        return token;
    }

    public String getLogdir() {
        return logdir;
    }

    public enum Type {
        dev, beta, prod
    }

    public static Server.Type typeOf(String hostname) {
        if (Strings.isNullOrEmpty(hostname) || hostname.contains("dev.")  || hostname.contains(".dev")) {
            return Server.Type.dev;
        }
        if (hostname.contains("beta.") || hostname.contains(".beta")) {
            return Server.Type.beta;
        }
        if (hostname.endsWith(".fovoy.com")) {
            return Server.Type.prod;
        }
        return Server.Type.dev;
    }

    public static String hostnameOf(String ip) {
        try {
            return InetAddress.getByName(ip).getHostName();
        } catch (UnknownHostException e) {
            return ip;
        }
    }
}
