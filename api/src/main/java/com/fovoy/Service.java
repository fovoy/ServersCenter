package com.fovoy;

import org.w3c.dom.Element;

/**
 * Created by zxz.zhang on 16/8/13.
 */
public interface Service {
    void init(String name, Element config) throws ServiceInitException;

    void destroy();

    public static class ServiceInitException extends Exception {


        private static final long serialVersionUID = -3672454559839962890L;

        public ServiceInitException() {
            super();
        }

        public ServiceInitException(String message, Throwable cause) {
            super(message, cause);
        }

        public ServiceInitException(String message) {
            super(message);
        }

        public ServiceInitException(Throwable cause) {
            super(cause);
        }

    }
}
