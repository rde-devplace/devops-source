package com.mibottle.ideoperators.util;

import com.mibottle.ideoperators.customresource.IdeConfigSpec;

public class SVCPathGenerator {
    public static String generatePath(IdeConfigSpec spec) {
        String userName = spec.getUserName();
        String wsName = spec.getWsName();
        String appName = spec.getAppName();

        if (wsName == null) {
            if (appName == null) {
                return "/" + userName;
            } else {
                return "/" + userName + "/" + appName;
            }
        } else {
            if (appName == null) {
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

        if (wsName == null) {
            if (appName == null) {
                return userName;
            } else {
                return userName + "-" + appName;
            }
        } else {
            if (appName == null) {
                return userName + "-" + wsName;
            } else {
                return userName + "-" + wsName + "-" + appName;
            }
        }
    }
}
