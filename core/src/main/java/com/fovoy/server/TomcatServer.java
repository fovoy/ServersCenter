package com.fovoy.server;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.ServletContext;
import java.lang.management.ManagementFactory;
import java.util.Set;

/**
 * Created by zxz.zhang on 16/8/13.
 */
public class TomcatServer extends ServerWrapper {

    public TomcatServer(ServletContext context) {
        super(context);
    }
    @Override
    protected void extractContext(ServletContext context) {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        try {
            Set<ObjectName> names = server.queryNames(new ObjectName("Catalina:type=Connector,*"), null);
            for (ObjectName name : names) {
                String protocol = server.getAttribute(name, "protocol").toString();
                if (protocol != null && (protocol.startsWith("HTTP/") || protocol.equals("org.apache.coyote.http11.Http11NioProtocol"))) {
                    port = Integer.parseInt(server.getAttribute(name, "port").toString());
                    return;
                }
            }
        } catch (Throwable e) {
            logger.warn("failed to get server port", e);
        }
        logger.warn("服务端口探测失败，当前MBean配置如下");
        for (ObjectName object : server.queryNames(null, null)) {
            try {
                MBeanAttributeInfo[] attrs = server.getMBeanInfo(object).getAttributes();
                for (MBeanAttributeInfo attr : attrs) {
                    try {
                        String name = attr.getName();
                        Object value = server.getAttribute(object, name);
                        logger.warn("name={}, type={}, value={}", name, attr.getType(), value);
                    }catch (Throwable t) {
                        logger.warn(object.getCanonicalName(), t);
                    }
                }
            } catch (Throwable e) {
                logger.warn(object.getCanonicalName(), e);
            }
        }
    }
}
