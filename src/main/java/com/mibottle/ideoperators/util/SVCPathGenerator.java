package com.mibottle.ideoperators.util;

import com.mibottle.ideoperators.customresource.IdeConfigSpec;

public class SVCPathGenerator {
    public static String generatePath(IdeConfigSpec spec) {
        String userName = spec.getUserName();
        String wsName = spec.getWsName();
        String appName = spec.getAppName();

        if (wsName == null || wsName.isEmpty()) {
            if (appName == null || appName.isEmpty()) {
                return "/" + userName;
            } else {
                return "/" + userName + "/" + appName;
            }
        } else {
            if (appName == null || appName.isEmpty()) {
                return "/" + userName + "/" + wsName;
            } else {
                return "/" + userName + "/" + wsName + "/" + appName;
            }
        }
    }


    public static String generateName(IdeConfigSpec spec) {
        String userName = spec.getUserName();
        String wsName = spec.getWsName();
        String appName = spec.getAppName();

        return generateName(userName, wsName, appName);
    }

    public static String generateName(String userName, String wsName, String appName) {
        if (wsName == null || wsName.isEmpty()) {
            if (appName == null || appName.isEmpty()) {
                return userName;
            } else {
                return userName + "-" + appName;
            }
        } else {
            if (appName == null || appName.isEmpty()) {
                return userName + "-" + wsName;
            } else {
                return userName + "-" + wsName + "-" + appName;
            }
        }
    }

}
