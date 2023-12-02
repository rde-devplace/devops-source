package com.mibottle.ideoperators.service;

import com.mibottle.ideoperators.customresource.IdeConfig;
import com.mibottle.ideoperators.customresource.IdeRole;
import com.mibottle.ideoperators.exception.IdeResourceDeleteException;
import com.mibottle.ideoperators.model.IdeCommon;
import com.mibottle.ideoperators.reconciler.IdeResourceGenerator;
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
        String serviceAccountName = resource.getSpec().getUserName() + IdeCommon.SA_NAME_POSTFIX;
        String labelName = resource.getSpec().getUserName() + IdeCommon.LABEL_NAME_POSTFIX;
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
                            resource.getSpec().getUserName() + IdeCommon.ROLE_BINDING_NAME_POSTFIX,
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
                            resource.getSpec().getUserName() + IdeCommon.CLUSER_ROLE_BINDING_NAME_POSTFIX,
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

        String secretName = resource.getSpec().getUserName() + IdeCommon.SECRET_NAME_POSTFIX;
        String labelName = resource.getSpec().getUserName() + IdeCommon.LABEL_NAME_POSTFIX;
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
                                                      Boolean isGit) {
        String statefulSetName = resource.getSpec().getUserName() + IdeCommon.STATEFULSET_NAME_POSTFIX;
        String serviceName = resource.getSpec().getUserName() + IdeCommon.SERVICE_NAME_POSTFIX;
        String labelName = resource.getSpec().getUserName() + IdeCommon.LABEL_NAME_POSTFIX;
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
                isGit
        );

        io.fabric8.kubernetes.api.model.Service service = ideResourceGenerator.serviceForIDE(
                resource,
                serviceName,
                labelName
        );

        // StatefulSet 생성
        try {
            log.info("Creating or replacing StatefulSet: " + statefulSet.getMetadata().getName() + " in namespace: " + namespace);
            client.apps().statefulSets().inNamespace(namespace).resource(statefulSet).serverSideApply();
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
            client.services().inNamespace(namespace).resource(service).serverSideApply();
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
        String statefulSetName = resource.getSpec().getUserName() + IdeCommon.STATEFULSET_NAME_POSTFIX;
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
        String serviceName = resource.getSpec().getUserName() + IdeCommon.SERVICE_NAME_POSTFIX;
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
        String secretName = resource.getSpec().getUserName() + IdeCommon.SECRET_NAME_POSTFIX;
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
        String serviceAccountName = resource.getSpec().getUserName() + IdeCommon.SA_NAME_POSTFIX;
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
        String roleBindingName = resource.getSpec().getUserName() + IdeCommon.ROLE_BINDING_NAME_POSTFIX;
        try {
            if (permissionScope.isPresent() && permissionScope.get().equals(IdeCommon.WEBSSH_PERMISSION_SCOPE_CLUSTER)) {
                roleBindingName = resource.getSpec().getUserName() + IdeCommon.CLUSER_ROLE_BINDING_NAME_POSTFIX;
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
