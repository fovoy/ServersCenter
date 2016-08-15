package com.fovoy;

import com.fovoy.concurrent.Closer;
import com.fovoy.concurrent.ManagedExecutors;
import com.fovoy.management.ServerManagement;
import com.fovoy.management.ServiceRecycle;
import com.fovoy.util.FovoyFileUtil;
import com.fovoy.util.LocalHost;
import com.fovoy.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by zxz.zhang on 16/8/13.
 */
public class FovoyServer {

    private static final Logger log = LoggerFactory.getLogger(FovoyServer.class);

    private static String configFile = "fovoy.conf";

    private static String conf_dir = null;

    enum ServerStatus {
        SERVER_STATUS_PENDING(0), SERVER_STATUS_STARTED(1), SERVER_STATUS_STARTING(2), SERVER_STATUS_STOPPING(3);
        private int code;

        ServerStatus(int code) {
            this.code = code;
        }
    }

    private static volatile ServerStatus SERVER_STATUS = ServerStatus.SERVER_STATUS_PENDING;
    private static final Map<String, Service> services = new LinkedHashMap<String, Service>();
    private static final Map<String, Object> resources = new LinkedHashMap<String, Object>();

    public static String getFovoyStore() {
        return FovoyFileUtil.getFovoyStore();
    }

    private static ExecutorManager em = null;

    public synchronized static void ServerStart(){
        if (SERVER_STATUS != ServerStatus.SERVER_STATUS_PENDING) {
            return;
        }
        long serverStart = System.currentTimeMillis();
        SERVER_STATUS = ServerStatus.SERVER_STATUS_STARTED;
        log.info("fovoy server System starting...");
        configFile = System.getProperty("fovoy.conf", configFile);
        File exf = new File("/~FovoyServer.disabled");
        try {
            if (conf_dir != null) {
                exf = new File(conf_dir, configFile);
                if (!exf.exists()) {
                    log.error("无法在此目录下找到配置文件" + configFile + ":" + conf_dir);
                }
            } else {
                URL url = FovoyServer.class.getResource("/" + configFile);
                if (url == null) {
                    log.info("没有找到配置文件: " + FovoyServer.class.getResource("/") + configFile);
                } else {
                    exf = new File(url.toURI());
                }
            }

            if (!exf.exists()) {
                exf = null;
                conf_dir = new File(FovoyServer.class.getResource("/").toURI()).getCanonicalPath();
            } else {
                exf = exf.getCanonicalFile();
                conf_dir = exf.getParent();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogUtil.log();
        LogUtil.log(LocalHost.getHostName());
        LogUtil.log();
        LogUtil.log("Fovoy ConfPath  : " + conf_dir);
        LogUtil.log("Fovoy Configure : " + exf);
        LogUtil.log("Fovoy StorePath : " + getFovoyStore());

        if (exf != null)
            initConfig(exf);
        LogUtil.log();
        log.info("fovoyServer core service init wasted in " + (System.currentTimeMillis() - serverStart));
        SERVER_STATUS = ServerStatus.SERVER_STATUS_STARTED;
    }

    @SuppressWarnings("unchecked")
    private static void initConfig(File exf) {
        FileInputStream confin = null;
        NodeList jdbcList = null;
        NodeList serviceList = null;
        NodeList executorList = null;
        try {
            confin = new FileInputStream(exf);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new InputStreamReader(confin, "UTF-8")));
            XPathFactory factory = XPathFactory.newInstance();
            executorList = (NodeList) factory.newXPath().compile("/server/executors").evaluate(doc, XPathConstants.NODESET);
            jdbcList = (NodeList) factory.newXPath().compile("/server/jdbc").evaluate(doc, XPathConstants.NODESET);
            serviceList = (NodeList) factory.newXPath().compile("/server/service").evaluate(doc, XPathConstants.NODESET);
        } catch (Throwable e) {
            log.error("fovoyServer 启动失败 ：配置文件解析错误！" + configFile, e);
            if (!LocalHost.isBeta()) throw new IllegalStateException("fovoyServer 启动失败 ：配置文件解析错误！" + configFile, e);
        } finally {
            Closer.close(confin);
        }
        if (executorList.getLength() > 0) {
            try {
                em = new ExecutorManager((Element) executorList.item(0));
            } catch (Throwable e) {
                log.error("ExecutorManager 加载失败 ", e);
                if (!LocalHost.isBeta()) throw new IllegalStateException("ExecutorManager 加载失败 " + configFile, e);
            }
        }
        if (jdbcList.getLength() > 0) {
            ObjectFactory factory = null;
            try {
                factory = (ObjectFactory) Class.forName("fovoy.sql.DataSourceFactory").newInstance();
            } catch (Exception e) {
                log.error("DataSourceFactory 加载失败 ", e);
            }

            for (int i = 0; i < jdbcList.getLength(); i++) {
                Element el = (Element) jdbcList.item(i);
                String name = "jdbc/" + el.getAttribute("name");
                if (resources.containsKey(name)) throw new IllegalArgumentException("连接池别名冲突 :" + name
                        + "已经被其它连接占用,请检查配置文件");
                try {
                    Reference ref = new Reference("fovoy.sql.GenericDataSource", "fovoy.sql.DataSourceFactory", null);
                    setParameter(ref, el.getAttributes());
                    resources.put(name, factory.getObjectInstance(ref, null, null, null));
                    log.info("资源[" + name + "]初始化成功");
                } catch (Throwable t) {
                    log.error(t.getMessage(), t);
                }
            }
        }

        for (int i = 0; i < serviceList.getLength(); i++) {
            Element el = (Element) serviceList.item(i);
            String serviceName = el.getAttribute("name");
            String className = el.getAttribute("class");
            try {
                Class<Service> clazz = (Class<Service>) Class.forName(className);
                Service service = clazz.newInstance();
                service.init(serviceName, el);
                log.info("服务[" + serviceName + "]启动成功");
                services.put(serviceName, service);
            } catch (ClassNotFoundException e) {
                log.error("服务载入错误 [" + serviceName + "]:无法找到类文件" + className, e);
                if (!LocalHost.isBeta()) throw new IllegalStateException("服务载入错误 [" + serviceName + "]:无法找到类文件"
                        + className, e);
            } catch (InstantiationException e) {
                log.error("服务载入错误 [" + serviceName + "]:类文件初始化错误,缺少公用构造方法." + className, e);
                if (!LocalHost.isBeta()) throw new IllegalStateException("服务载入错误 [" + serviceName
                        + "]:类文件初始化错误,缺少公用构造方法." + className, e);
            } catch (IllegalAccessException e) {
                log.error("服务载入错误 [" + serviceName + "]:类文件初始化错误,缺少公用构造方法." + className, e);
                if (!LocalHost.isBeta()) throw new IllegalStateException("服务载入错误 [" + serviceName
                        + "]:类文件初始化错误,缺少公用构造方法." + className, e);
            } catch (Throwable e) {
                log.error("服务启动失败 [" + serviceName + "]:" + className, e);
                if (!LocalHost.isBeta()) throw new IllegalStateException("服务启动失败[" + serviceName + "]:" + className, e);
            }
        }
    }

    public synchronized static void ServerStop() {
        if (SERVER_STATUS != ServerStatus.SERVER_STATUS_STARTED) return;
        SERVER_STATUS = ServerStatus.SERVER_STATUS_STOPPING;
        log.info("FovoyServer core service stoping");

        ManagedExecutors.getScheduleExecutor().shutdown();

        stopServices();

        ServerManagement sm = ServersContainer.getService(ServerManagement.class);
        if(sm != null && sm instanceof ServiceRecycle){
            try {
                ((ServiceRecycle) sm).destroy();
            }catch (Throwable t){
                log.info("ServerManagement[" + sm + "]已释放");
            }
        }

        closeResources();

        if (em != null) em.destroy();

        ManagedExecutors.getExecutor().shutdown();

        log.info("FovoyServer core service shutdown");
        SERVER_STATUS = ServerStatus.SERVER_STATUS_PENDING;
    }
    private static void closeResources() {
        @SuppressWarnings("unchecked")
        Map.Entry<String, Closeable>[] arr1 = new Map.Entry[resources.size()];
        resources.entrySet().toArray(arr1);
        for (int i = arr1.length - 1; i >= 0; i--) {
            Map.Entry<String, Closeable> entry = arr1[i];
            try {
                entry.getValue().close();
                log.info("资源[" + entry.getKey() + "]已释放");
            } catch (Throwable t) {
                log.error("资源[" + entry.getKey() + "]在释放时遇到一个错误 " + t.getMessage(), t);
            }
        }
        resources.clear();
    }
    private static void stopServices() {
        @SuppressWarnings("unchecked")
        Map.Entry<String, Service>[] arr = new Map.Entry[services.size()];
        services.entrySet().toArray(arr);
        for (int i = arr.length - 1; i >= 0; i--) {
            Map.Entry<String, Service> entry = arr[i];
            try {
                entry.getValue().destroy();
                log.info("服务[" + entry.getKey() + "]已关闭");
            } catch (Throwable t) {
                log.error("服务[" + entry.getKey() + "]在关闭时遇到一个错误 " + t.getMessage(), t);
            }
        }
        services.clear();
    }

    private static void setParameter(Reference ref, NamedNodeMap map) {
        for (int i = 0; i < map.getLength(); i++) {
            Node node = map.item(i);
            ref.add(new StringRefAddr(node.getNodeName(), node.getNodeValue()));
        }
    }
}
