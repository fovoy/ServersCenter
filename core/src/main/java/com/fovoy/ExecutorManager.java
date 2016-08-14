package com.fovoy;

import com.fovoy.concurrent.FovoyThreadFactory;
import com.fovoy.concurrent.ManagedThreadPool;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.LinkedHashMap;
import java.util.concurrent.*;

/**
 * Created by zxz.zhang on 16/8/14.
 */
public class ExecutorManager {

    private LinkedHashMap<String, ThreadPoolExecutor> executors = new LinkedHashMap<String, ThreadPoolExecutor>();

    ExecutorManager(Element config) {

        NodeList nodes = config.getElementsByTagName("executor");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String name = el.getAttribute("name");
            int corePoolSize = Integer.parseInt(el.getAttribute("corePoolSize"));
            int maximumPoolSize = Integer.parseInt(el.getAttribute("maximumPoolSize"));
            long keepAliveTime = Long.parseLong(el.getAttribute("keepAliveTime"));

            // 以下为可选参数
            int queueSize = Integer.valueOf(el.getAttribute("queueSize"), 0);
            boolean rejectDiscard = Boolean.valueOf(el.getAttribute("rejectDiscard"));

            BlockingQueue<Runnable> queue;
            if (queueSize == 0)
                queue = new SynchronousQueue<Runnable>();
            else
                queue = new ArrayBlockingQueue<Runnable>(queueSize);

            ManagedThreadPool pool = new ManagedThreadPool(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, queue, new FovoyThreadFactory(name));
            if (rejectDiscard)
                pool.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());

            executors.put(name, pool);

//            Sugar.pool("com.fovoy.core:type=ThreadPool,name=" + name, pool);

        }

    }

    void destroy() {
        for (ThreadPoolExecutor exec : executors.values()) {
            try {
                exec.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    ThreadPoolExecutor getExecutor(String name) {
        return executors.get(name);
    }
}
