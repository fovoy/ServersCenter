package com.fovoy;

import com.fovoy.util.VersionUtil;

/**
 * Created by zxz.zhang on 16/8/13.
 */
public interface Constants {

    interface Center {
        String KEY_CENTER_SERVER = "cserver.host";
        String URL_CENTER_SERVER = "cserver.fovoy.com";
        String CENTER_LOCAL = "#";

    }

    interface Common{
        String VERSION = VersionUtil.getVersion(Constants.class, "");
    }

    interface HEADER {
        String HEADER_X_REAL_IP = "X-Real-IP";
        String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";
        String CONTENT_TYPE_TEXT = "text/plain;charset=UTF-8";
        String HEADER_VERSION = "Q-Version";
        String HEADER_SERVER_TOKEN = "Q-Server-Token";
        String HEADER_APP_CODE = "Q-App-Code";
    }
}
