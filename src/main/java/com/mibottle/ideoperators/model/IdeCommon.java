/*
 * Copyright (c) 2023 himang10@gmail.com, Yi Yongwoo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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


