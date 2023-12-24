package com.mibottle.ideoperators.model;

public final class IdeCommon {

    private IdeCommon() {
    }

    // POSTFIX
    public static final String SECRET_NAME_POSTFIX="-ide-secret";
    public static final String SA_NAME_POSTFIX="-ide-account";
    public static final String ROLE_BINDING_NAME_POSTFIX="-ide-rolebinding";
    public static final String CLUSER_ROLE_BINDING_NAME_POSTFIX="-ide-cluster-rolebinding";

    public static final String SERVICE_NAME_POSTFIX="-vscode-server-service";
    public static final String STATEFULSET_NAME_POSTFIX="-vscode-server-statefulset";
    public static final String LABEL_NAME_POSTFIX="-vscode-server";

    public static final String IDECONFIG_KIND = "IdeConfig";
    public static final String IDECONFIG_GROUP = "amdev.cloriver.io";

    public static final String IDECONFIG_CRD_NAME = "ideconfigs.amdev.cloriver.io";
    public static final String IDECONFIG_CRD_PLURAL = "ideconfigs";

    public static final String IDECONFIG_POSTFIX = "-vscode-server";

    //Git configuration
    public static final String GIT_ID = "id";
    public static final String GIT_TOKEN = "token";
    public static final String GIT_REPOSITORY = "repository";
    public static final String GIT_BRANCH = "branch";
    public static final String SERVICE_ACCOUNT_NAME = "serviceAccountName";


    // PVC
    public static final String USER_DEV_STORAGE = "user-dev-storage";
    public static final String INIT_COMM_STORAGE = "com-dev-storage";
    public static final String USER_DEV_STORAGE_ACCESS_MODE = "ReadWriteOnce";

    // WEBSSH_PERMISSION
    public static final String WEBSSH_PERMISSION_SCOPE_NAMESPACE = "namespace";
    public static final String WEBSSH_PERMISSION_SCOPE_CLUSTER = "cluster";
    public static final String DEFAULT_SERVICE_ACCOUNT_NAME = "default";

}


