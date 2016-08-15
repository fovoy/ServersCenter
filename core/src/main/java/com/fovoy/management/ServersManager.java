package com.fovoy.management;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fovoy.Server;
import com.fovoy.ServerConfig;
import com.fovoy.ServersContainer;
import com.fovoy.concurrent.Closer;
import com.fovoy.concurrent.Files;
import com.fovoy.io.CachedRemoteFile;
import com.fovoy.json.CodeMessage;
import com.fovoy.json.MapperBuilder;
import com.fovoy.json.node.MapNode;
import com.fovoy.json.node.ValueNode;
import com.fovoy.rest.RestController;
import com.fovoy.util.*;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.Key;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static com.fovoy.Constants.Center.*;
import static com.fovoy.Constants.Common.*;

/**
 * Created by zxz.zhang on 16/8/12.
 */
public class ServersManager implements ServerManagement,ServiceRecycle {
    private static Logger logger = LoggerFactory.getLogger(ServersManager.class);

    private AtomicBoolean isSetupHealthCheckPoller = new AtomicBoolean(false);

    private static Supplier<ServerManagement> holder = Suppliers.memoize(() -> ServersContainer.getService(ServerManagement.class));

    private static AtomicReference<ServersManager> instance = new AtomicReference();

    private static final HealthyChecker HEALTHY_CHECKER = new HealthyChecker();
    private final ArrayList<ServiceRecycle> recycles = new ArrayList<ServiceRecycle>();

    private final ServerConfig config;

    private final CachedRemoteFile keyFile;

    private final Key key;

    public ServersManager() {
        if (!instance.compareAndSet(null, this))
            throw new IllegalStateException("ServerManager只能被初始化一次.");

        config = initAppConfig();
        LogUtil.log();
        LogUtil.log("APP CONFIG");
        LogUtil.log();
        LogUtil.log("Center       : ", getCenter());
        LogUtil.log("Organization : ", config.getOrganization());
        LogUtil.log("App Name     : ", config.getName());
        LogUtil.log("Server Type  : ", config.getServer().getType());
        LogUtil.log("Server Room  : ", config.getServer().getRoom());
        LogUtil.log("Server IP    : ", config.getServer().getIp());
        LogUtil.log("Server Host  : ", config.getServer().getHostname());
        LogUtil.log("Server PID   : ", config.getServer().getPid());
        LogUtil.log("Version      : ", VERSION);
        LogUtil.log();

        String center = config.getEnv().get(KEY_CENTER_SERVER);
        String serverToken = config.getServer().getToken();
        keyFile = initKey(Strings.isEmpty(center) || CENTER_LOCAL.equals(center) ? URL_CENTER_SERVER : center, serverToken);
        key = loadKey(keyFile.getFile());

    }

    public static ServersManager getInstance() {
        holder.get();
        return instance.get();
    }

    @Override
    public void destroy() {

    }

    public String getCenter() {
        return config.getEnv().get(KEY_CENTER_SERVER);
    }

    private final AtomicReference<RestController> handlers = new AtomicReference();

    private final ConcurrentMap<String, InfoReporter> reporters = Maps.newConcurrentMap();


    /**
     * <pre>
     * 启动主要过程
     * 1) 读取本地Fovoy-app.properties配置
     * 2) 启用中心
     *    => 禁用: 使用定制的配置
     *    => 否则: 执行 3)
     * 3)
     * 3.1) 使用app token请求server token
     *      => 失败: 使用历史配置
     *      => 成功: 保存配置
     * 3.2) 注册主机信息
     * </pre>
     */
    private static ServerConfig initAppConfig() {

        // 运行时数据
        int pid = getPid();
        String ip = LocalHost.getLocalHost();
        String logdir = null;
        String basePath = System.getProperty("catalina.base");
        if (basePath != null) {
            File logDirFile = new File(basePath, "logs");
            if (logDirFile.exists()) {
                logdir = logDirFile.getAbsolutePath();
            }
        }
        Map<String, String> env = new HashMap<String, String>();

        ResourceConfig app = ResourceConfig.getOrNull("Fovoy-app.properties");
        if (app == null) {
            logger.error("加载应用配置 Fovoy-app.properties 失败, 请参考 http://wiki.github.com");
            return new ServerConfig(null, null, null, new Server(null, pid, null, null, null, ip, 0, logdir, null), env);
        }

        // 应用级数据，各环境一致
        String organization = app.getString("organization");
        String name = app.getString("name");
        String token = app.getString("token");
        int port = app.getInt("server.port", 0);
        String hostname = app.getString("server.hostname");
        String room = app.getString("server.room");
        String server_token = app.getString("server.token");

        // 各环境不一致的数据
        ResourceConfig _env = ResourceConfig.getOrNull("fovoy-env.properties");
        if (_env != null) {
            env.putAll(_env.getAll());
        }
        Server.Type type = typeOf(env);

        String center = env.get(KEY_CENTER_SERVER);
        if (CENTER_LOCAL.equals(center)) {
            LogUtil.log("!!!警告，应用中心已禁用，使用本地调试模式. ");
            return new ServerConfig(organization, name, token,
                    new Server(name, pid, type, hostname, room, ip, port, logdir, server_token), env);
        }

        if (Strings.isEmpty(center)) {
            center = URL_CENTER_SERVER;
        }

        File file = new File(CachedRemoteFile.CACHE_HOME, "serverConfig");

        Map<String, Object> param = new HashMap<String, Object>();
        param.put("token", token);
        param.put("server.pid", pid);
        if (type != null) {
            param.put("userdefine.env", type);
            logger.info("用户自定义环境为: [" + type + "]");
        }

        ValueNode resp = RequestUtil.post(center + "/api/app/info.json", param);
        if (resp.get("status").getIntValue(CodeMessage.SYSTEM_ERROR) != CodeMessage.OK) {
            logger.error("应用中心请求失败: {}", resp.get("message"));

            if (!file.exists()) {
                return new ServerConfig(organization, name, token,
                        new Server(name, pid, type, hostname, room, ip, port, logdir, server_token), env);
            }

            logger.warn("读取历史配置: {}", file);
            try {
                return MapperBuilder.getDefaultMapper().readValue(new FileReader(file), new TypeReference<ServerConfig>() {
                });
            } catch (IOException e) {
                logger.error("读取历史应用配置失败 " + file, e);
            }
        }

        MapNode data = (MapNode) resp.get("data");
        organization = data.get("organization").toString();
        name = data.get("name").toString();
        type = Server.Type.valueOf(data.get("server.type").toString());
        ip = data.get("server.ip").toString();
        hostname = data.get("server.hostname").toString();
        room = data.get("server.room").toString();
        server_token = data.get("server.token").toString();
        if (!env.containsKey(KEY_CENTER_SERVER)) {
            env.put(KEY_CENTER_SERVER, data.get("env.registry").toString());
        }

        ServerConfig config = new ServerConfig(organization, name, token,
                new Server(name, pid, type, hostname, room, ip, port, logdir, server_token), env);

        try {
            MapperBuilder.getDefaultMapper().writeValue(new FileWriter(file), config);
        } catch (IOException e) {
            logger.warn("无法保存应用配置 " + file, e);
        }

        return config;
    }

    private static Server.Type typeOf(Map<String, String> env) {
        String name = env.get("name");

        if (Strings.isEmpty(name)) {
            name = System.getProperty("fovoy.env");
        }

        if (Strings.isEmpty(name)) {
            name = System.getenv("fovoy.env");
        }

        return Strings.isEmpty(name) ? null : Server.Type.valueOf(name);
    }

    public static int getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.valueOf(name.substring(0, name.indexOf('@')));
    }

    public synchronized void setupHealthCheckPoller(final Supplier<String> pathGetter) {
        if (isSetupHealthCheckPoller.get()) return;

        isSetupHealthCheckPoller.compareAndSet(false, true);
//        HEALTHY_CHECKER.start(pathGetter);  // TODO: 16/8/13
    }
    private static Key loadKey(File file) {
        try {
            return Files.readObject(file);
        } catch (Exception e) {
            logger.error("无法读取密钥文件 {}", file);
        }
        return null;
    }

    private static CachedRemoteFile initKey(String center, String serverToken) {
        String location = "http://" + center + "/config/secretKey?server_token=" + Strings.encodeURL(serverToken);
        CachedRemoteFile file = new CachedRemoteFile("secretKey", location, true);
        file.addListener(new CachedRemoteFile.Listener() {
            @Override
            public void onUpdate(CachedRemoteFile cache) {
                loadKey(cache.getFile());
            }
        });

        return file;
    }

    public void initRestController(final String contextPath) {
        handlers.compareAndSet(null, new RestController(contextPath));
    }

    public RestController getHandlers() {
        // 防止非tomcat项目中handlers未初始化时直接调用getHandlers().addRequestHandler出错。
        handlers.compareAndSet(null, new RestController());
        return handlers.get();
    }
    public boolean healthCheck() {
        return HEALTHY_CHECKER.healthCheck();
    }

    public ServerConfig getConfig() {
        return config;
    }

    private static class DuplicateConfigException extends RuntimeException {
        private static final long serialVersionUID = -2971027482886115437L;

        DuplicateConfigException(final String message) {
            super(message);
        }
    }
    // 用于从classpath或者jar包中载入fovoy-app.properties文件或者fovoy-env.properties文件
    private static class ResourceConfig {
        private final Map<String, String> data;

        static ResourceConfig getOrNull(final String name) {
            try {
                return new ResourceConfig(name);
            } catch (DuplicateConfigException e) {
                throw new RuntimeException("检测到重复配置文件", e);
            } catch (Exception e) {
                return null;
            }
        }

        private ResourceConfig(final String name) {
            Preconditions.checkArgument(!Strings.isEmpty(name), "配置文件名不能为空");
            forbidDuplicateConfig(name);

            final InputStream res = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
            if (res == null) {
                throw new RuntimeException("无法找到配置文件: " + name);
            }

            try {
                final Properties prop = new Properties();
                prop.load(res);
                data = fromProperties(prop);
            } catch (Exception e) {
                throw new RuntimeException("无法读取配置文件：" + name, e);
            } finally {
                Closer.close(res);
            }
        }

        private void forbidDuplicateConfig(final String name) {
            try {
                final List<URL> resources = Collections.list(Thread.currentThread().getContextClassLoader().getResources(name));
                if (resources.size() > 1) {
                    logger.error("文件{}只允许有一个，但是发现多个，位置分别为: {}", name, resources);
                    throw new DuplicateConfigException("配置文件" + name + "不能存在多个，地址分别为：" + resources);
                }
            } catch (IOException e) {
                // do nothing here
            }
        }

        private Map<String, String> fromProperties(final Properties prop) {
            final Map<String, String> map = Maps.newHashMap();
            for (final String key : prop.stringPropertyNames()) {
                map.put(key, prop.getProperty(key));
            }
            return map;
        }

        String getString(final String key) {
            return data.get(key);
        }

        int getInt(final String key, final int defaultValue) {
            return Numbers.toInt(data.get(key), defaultValue);
        }

        Map<String, String> getAll() {
            return Collections.unmodifiableMap(data);
        }
    }
}
