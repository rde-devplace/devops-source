package com.mibottle.ideoperators.service;
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
import com.mibottle.ideoperators.customresource.IdeConfig;
import com.mibottle.ideoperators.customresource.IdeConfigSpec;
import com.mibottle.ideoperators.customresource.IdeRole;
import com.mibottle.ideoperators.exception.IdeResourceDeleteException;
import com.mibottle.ideoperators.model.IdeCommon;
import com.mibottle.ideoperators.reconciler.IdeResourceGenerator;
import com.mibottle.ideoperators.util.SVCPathGenerator;
import com.mibottle.ideoperators.util.UpdateStatusHandler;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class IdeResourceService {

    private final IdeResourceGenerator ideResourceGenerator;
    private final KubernetesClient client;

    @Value("${ide.sshserver.clusterRole.admin:cluster-admin}")
    private String clusterRoleAdmin;
    @Value("${ide.sshserver.clusterRole.view:view}")
    private String clusterRoleView;

    public IdeResourceService(KubernetesClient client, IdeResourceGenerator ideResourceGenerator) {
        this.ideResourceGenerator = ideResourceGenerator;
        this.client = client;
    }

    /**
     * ServiceAccount를 구성하고 적절한 Role을 Binding한다.
     *
     * @param resource
     * @param namespace
     * @return
     */
    public Optional<String> createOrUpdateAuthentication(IdeConfig resource, String namespace, Boolean isWebssh) {
        IdeConfigSpec spec = resource.getSpec();
        String serviceAccountName = SVCPathGenerator.generateName(spec) + IdeCommon.SA_NAME_POSTFIX;
        String labelName = SVCPathGenerator.generateName(spec) + IdeCommon.LABEL_NAME_POSTFIX;
        if(!isWebssh) {
            log.info("Webssh is not exist");
            return Optional.ofNullable(IdeCommon.DEFAULT_SERVICE_ACCOUNT_NAME);
        }

        if (resource.getSpec().getWebssh().getPermission().getUseType().equals("create")) {
            /**
             * 1.1. ServiceAccount 생성
             */
            ServiceAccount sa = ideResourceGenerator.serviceAccountForIDE(
                    resource,
                    serviceAccountName,
                    labelName
            );
            try {
                log.info("Creating or replacing ServiceAccount: " + sa.getMetadata().getName() + " in namespace: " + namespace);
                client.serviceAccounts().inNamespace(namespace).resource(sa).serverSideApply();
            } catch (KubernetesClientException e) {
                log.error("Failed to create or replace ServiceAccount: " + e.getMessage());
                // 적절한 예외 처리 로직
                UpdateStatusHandler.updateStatus(
                        resource,
                        "Failed to create or replace ServiceAccount: " + e.getMessage(),
                        false);

                //return UpdateControl.updateStatus(resource);
                return Optional.empty();
            }

            IdeRole ideRole = ideResourceGenerator.getIdeRole(resource, this.clusterRoleAdmin, this.clusterRoleView);
            switch (ideRole.getRoleBinding()) {
                case "RoleBinding":
                    RoleBinding role = ideResourceGenerator.roleBindingForIDE(
                            resource,
                            ideRole,
                            SVCPathGenerator.generateName(resource.getSpec()) + IdeCommon.ROLE_BINDING_NAME_POSTFIX,
                            labelName,
                            serviceAccountName
                    );
                    try {
                        log.info("Creating or replacing RoleBinding: " + role.getMetadata().getName() + " in namespace: " + namespace);
                        client.rbac().roleBindings().inNamespace(namespace).resource(role).serverSideApply();
                    } catch (KubernetesClientException e) {
                        log.error("Failed to create or replace RoleBinding: " + e.getMessage());
                        // 적절한 예외 처리 로직
                        UpdateStatusHandler.updateStatus(
                                resource,
                                "Failed to create or replace RoleBinding: " + e.getMessage(),
                                false);

                        //return UpdateControl.updateStatus(resource);
                        return Optional.empty();
                    }
                    break;
                case "ClusterRoleBinding":
                    ClusterRoleBinding clusterRole = ideResourceGenerator.clusterRoleBindingForIDE(
                            resource,
                            ideRole,
                            SVCPathGenerator.generateName(resource.getSpec()) + IdeCommon.CLUSTER_ROLE_BINDING_NAME_POSTFIX,
                            labelName,
                            serviceAccountName
                    );
                    try {
                        log.info("Creating or replacing ClusterRoleBinding: " + clusterRole.getMetadata().getName() + " in namespace: " + namespace);
                        client.rbac().clusterRoleBindings().resource(clusterRole).serverSideApply();
                    } catch (KubernetesClientException e) {
                        log.error("Failed to create or replace ClusterRoleBinding: " + e.getMessage());
                        // 적절한 예외 처리 로직
                        UpdateStatusHandler.updateStatus(
                                resource,
                                "Failed to create or replace ClusterRoleBinding: " + e.getMessage(),
                                false);

                        //return UpdateControl.updateStatus(resource);
                        return Optional.empty();
                    }
                    break;
                default:
                    log.info("RoleBinding is null or empty");
                    return Optional.empty();
            }
        } else if (!resource.getSpec().getWebssh().getPermission().getServiceAccountName().isEmpty()) {
            // ServiceAccount 사용
            serviceAccountName = resource.getSpec().getWebssh().getPermission().getServiceAccountName();
            ServiceAccount sa = client.serviceAccounts().inNamespace(namespace).withName(serviceAccountName).get();
            if (sa == null) {
                log.info("serviceAccount is null");
                return Optional.empty();
            }
        } else {
            log.info("serviceAccountName is null or empty");
            return Optional.empty();
        }

        return Optional.ofNullable(serviceAccountName);
    }

    /**
     * Git Secret 정보를 생성한다
     *
     * @param resource
     * @param namespace
     * @param isGit
     * @return
     */
    public Optional<String> createOrUpdateSecret(IdeConfig resource, String namespace, Boolean isVscode, Boolean isGit) {
        if (!isVscode ) {
            log.debug("Vscode is disabled");
            return Optional.of("isNotVscode");
        } else if (!isGit) {
            log.debug("Git is disabled");
            return Optional.of("isNotGit");
        }

        String name_prefix = SVCPathGenerator.generateName(resource.getSpec());
        String secretName = name_prefix + IdeCommon.SECRET_NAME_POSTFIX;
        String labelName = name_prefix + IdeCommon.LABEL_NAME_POSTFIX;
        Secret secret = ideResourceGenerator.secretForIDE(resource, secretName, labelName);
        try {
            log.info("Creating or replacing Secret: " + secret.getMetadata().getName() + " in namespace: " + namespace);
            client.secrets().inNamespace(namespace).resource(secret).serverSideApply();
        } catch (KubernetesClientException e) {
            log.error("Failed to create or replace Secret: " + e.getMessage());
            // 적절한 예외 처리 로직
            UpdateStatusHandler.updateStatus(
                    resource,
                    "Failed to create or replace Secret: " + e.getMessage(),
                    false);

            //return UpdateControl.updateStatus(resource);
            return Optional.empty();
        }

        return Optional.of(secretName);
    }


    /**
     * Statefulset과 Service를 생성한다.
     *
     * @param resource
     * @param namespace
     * @param containers
     * @param comDevPvc
     * @param storageClassNameForUser
     * @param serviceAccountName
     * @param isVscode
     * @param isGit
     * @return
     */
    public Optional<String> createOrUpdateStatefulset(IdeConfig resource,
                                                      String namespace,
                                                      List<Container> containers,
                                                      String comDevPvc,
                                                      String storageClassNameForUser,
                                                      String serviceAccountName,
                                                      Boolean isVscode,
                                                      Boolean isGit,
                                                      Boolean isNotebook) {
        String name_prefix = SVCPathGenerator.generateName(resource.getSpec());
        String statefulSetName = name_prefix + IdeCommon.STATEFULSET_NAME_POSTFIX;
        String serviceName = name_prefix + IdeCommon.SERVICE_NAME_POSTFIX;
        String labelName = name_prefix + IdeCommon.LABEL_NAME_POSTFIX;

        StatefulSet statefulSet = ideResourceGenerator.statefulSetForIDE(
                resource,
                statefulSetName,
                serviceName,
                labelName,
                containers,
                comDevPvc,
                storageClassNameForUser,
                serviceAccountName,
                isVscode,
                isGit,
                isNotebook
        );


        io.fabric8.kubernetes.api.model.Service service = ideResourceGenerator.serviceForIDE(
                resource,
                serviceName,
                labelName
        );

        // StatefulSet 생성
        try {
            log.info("Creating or replacing StatefulSet: " + statefulSet.getMetadata().getName() + " in namespace: " + namespace);
            StatefulSet currentStatefulset = client.apps().statefulSets().inNamespace(namespace).withName(statefulSetName).get();
            // Statefulset 현재 spec과 다를 경우에만 apply 적용
            if (currentStatefulset == null || !currentStatefulset.getSpec().equals(statefulSet.getSpec())) {
                client.apps().statefulSets().inNamespace(namespace).resource(statefulSet).serverSideApply();
            }
        } catch (KubernetesClientException e) {
            log.error("Failed to create or replace Service: " + e.getMessage());
            // 적절한 예외 처리 로직
            // Maybe log the error, set a status message and return
            UpdateStatusHandler.updateStatus(
                    resource,
                    "Failed to create or replace StatefulSet: " + e.getMessage(),
                    false);

            //return UpdateControl.updateStatus(resource);
            return Optional.empty();
        }

        // Service 생성
        try {
            log.info("Creating or replacing Service: " + service.getMetadata().getName() + " in namespace: " + namespace);

            // currentService를 가져온다
            io.fabric8.kubernetes.api.model.Service currentService = client.services().inNamespace(namespace).withName(serviceName).get();
            // Service 현재 spec과 다를 경우에만 apply 적용
            if (currentService == null || !currentService.getSpec().equals(service.getSpec())) {
                client.services().inNamespace(namespace).resource(service).serverSideApply();
            }
        } catch (KubernetesClientException e) {
            log.error("Failed to create or replace Service: " + e.getMessage());
            // 적절한 예외 처리 로직
            UpdateStatusHandler.updateStatus(
                    resource,
                    "Failed to create or replace StatefulSet: " + e.getMessage(),
                    false);

            return Optional.empty();
            //return UpdateControl.noUpdate();
        }

        //return UpdateControl.patchStatus(resource).rescheduleAfter(10, TimeUnit.SECONDS);
        return Optional.ofNullable(statefulSetName);
    }


    public Optional<String> deleteStatefulset(IdeConfig resource, String namespace) throws IdeResourceDeleteException {
        String name_prefix = SVCPathGenerator.generateName(resource.getSpec());
        String statefulSetName = name_prefix + IdeCommon.STATEFULSET_NAME_POSTFIX;
        try {
            client.apps().statefulSets().inNamespace(namespace).withName(statefulSetName).delete();
        } catch (KubernetesClientException e) {
            // 삭제 중 에러 발생 시 로직
            resource = UpdateStatusHandler.updateStatus(
                    resource,
                    "Failed to delete StatefulSet: " + e.getMessage(),
                    false);
            throw new IdeResourceDeleteException(resource, "Failed to delete StatefulSet", e);
        }
        return Optional.ofNullable(statefulSetName);
    }

    public Optional<String> deleteService(IdeConfig resource, String namespace) throws IdeResourceDeleteException {
        String name_prefix = SVCPathGenerator.generateName(resource.getSpec());
        String serviceName = name_prefix + IdeCommon.SERVICE_NAME_POSTFIX;
        try {
            client.services().inNamespace(namespace).withName(serviceName).delete();
        } catch (KubernetesClientException e) {
            // 삭제 중 에러 발생 시 로직
            resource = UpdateStatusHandler.updateStatus(
                    resource,
                    "Failed to delete Service: " + e.getMessage(),
                    false);
            throw new IdeResourceDeleteException(resource, "Failed to delete Service", e);
        }
        return Optional.ofNullable(serviceName);
    }

    public Optional<String> deleteSecret(IdeConfig resource, String namespace) throws IdeResourceDeleteException {
        String name_prefix = SVCPathGenerator.generateName(resource.getSpec());
        String secretName = name_prefix + IdeCommon.SECRET_NAME_POSTFIX;
        try {
            client.secrets().inNamespace(namespace).withName(secretName).delete();
        } catch (KubernetesClientException e) {
            // 삭제 중 에러 발생 시 로직
            resource = UpdateStatusHandler.updateStatus(
                    resource,
                    "Failed to delete Secret: " + e.getMessage(),
                    false);
            throw new IdeResourceDeleteException(resource, "Failed to delete Secret", e);
        }
        return Optional.ofNullable(secretName);
    }

    public Optional<String> deleteServiceAccount(IdeConfig resource, String namespace) throws IdeResourceDeleteException {
        String name_prefix = SVCPathGenerator.generateName(resource.getSpec());
        String serviceAccountName = name_prefix + IdeCommon.SA_NAME_POSTFIX;
        try {
            client.serviceAccounts().inNamespace(namespace).withName(serviceAccountName).delete();
        } catch (KubernetesClientException e) {
            // 삭제 중 에러 발생 시 로직
            resource = UpdateStatusHandler.updateStatus(
                    resource,
                    "Failed to delete ServiceAccount: " + e.getMessage(),
                    false);
            throw new IdeResourceDeleteException(resource, "Failed to delete ServiceAccount", e);
        }
        return Optional.ofNullable(serviceAccountName);
    }

    public Optional<String> deleteRoleBinding(IdeConfig resource, String namespace) throws IdeResourceDeleteException {
        Optional<String> permissionScope = Optional.ofNullable(resource.getSpec().getWebssh().getPermission().getScope());
        String name_prefix = SVCPathGenerator.generateName(resource.getSpec());
        String roleBindingName = name_prefix + IdeCommon.ROLE_BINDING_NAME_POSTFIX;
        try {
            if (permissionScope.isPresent() && permissionScope.get().equals(IdeCommon.WEBSSH_PERMISSION_SCOPE_CLUSTER)) {
                roleBindingName = name_prefix + IdeCommon.CLUSTER_ROLE_BINDING_NAME_POSTFIX;
                client.rbac().clusterRoleBindings().withName(roleBindingName).delete();
            } else {
                client.rbac().roleBindings().inNamespace(namespace).withName(roleBindingName).delete();
            }
        } catch (KubernetesClientException e) {
            // 삭제 중 에러 발생 시 로직
            resource = UpdateStatusHandler.updateStatus(
                    resource,
                    "Failed to delete RoleBinding: " + e.getMessage(),
                    false);
            throw new IdeResourceDeleteException(resource, "Failed to delete RoleBinding", e);
        }
        return Optional.ofNullable(roleBindingName);
    }
}
