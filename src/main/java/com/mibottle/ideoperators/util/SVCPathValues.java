package com.mibottle.ideoperators.util;

public class SVCPathValues {
    private String userName;
    private String wsName;
    private String appName;

    private SVCPathValues(String userName, String wsName, String appName) {
        this.userName = userName;
        this.wsName = wsName;
        this.appName = appName;
    }

    public String getUserName() {
        return userName;
    }

    public String getWsName() {
        return wsName;
    }

    public String getAppName() {
        return appName;
    }


    public static SVCPathValues extractValues(String path) {
        String[] parts = path.split("/");
        String userName = null;
        String wsName = null;
        String appName = null;

        if (parts.length > 1) {
            userName = parts[1];
        }
        if (parts.length > 2) {
            wsName = parts[2];
        }
        if (parts.length > 3) {
            appName = parts[3];
        }

        return new SVCPathValues(userName, wsName, appName);
    }
}
