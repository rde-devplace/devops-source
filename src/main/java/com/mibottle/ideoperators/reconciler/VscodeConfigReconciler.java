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
package com.mibottle.ideoperators.reconciler;

import com.mibottle.ideoperators.customresource.IdeConfig;
import com.mibottle.ideoperators.customresource.IdeConfigSpec;
import com.mibottle.ideoperators.customresource.IdeRole;
import com.mibottle.ideoperators.exception.IdeResourceDeleteException;
import com.mibottle.ideoperators.model.IdeCommon;
import com.mibottle.ideoperators.service.IdeConfigService;
import com.mibottle.ideoperators.service.IdeResourceService;
import com.mibottle.ideoperators.util.SVCPathGenerator;
import com.mibottle.ideoperators.util.UpdateStatusHandler;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.event.rate.RateLimited;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mibottle.ideoperators.util.UpdateStatusHandler.handleError;

/**
 * VscodeConfigReconciler는 Kubernetes 클러스터 내의 Visual Studio Code 서버를 관리하는 컨트롤러입니다.
 * 이 클래스는 Java Operator SDK를 사용하여 VS Code 서버의 생성, 업데이트 및 삭제를 담당합니다.
 * 이 클래스는 Reconciler, Cleaner, ErrorStatusHandler, 그리고 EventSourceInitializer 인터페이스를 구현합니다.
 */

@Slf4j
@RateLimited(maxReconciliations = 2, within = 3)
@ControllerConfiguration
public class VscodeConfigReconciler
        implements Reconciler<IdeConfig>, Cleaner<IdeConfig>,ErrorStatusHandler<IdeConfig>, EventSourceInitializer<IdeConfig>{
    //implements Reconciler<IdeConfig>, Cleaner<IdeConfig>, ErrorStatusHandler<IdeConfig>, EventSourceInitializer<IdeConfig> {

    private KubernetesClient client;
    private IdeResourceGenerator ideResourceGenerator;

    @Autowired
    private IdeResourceService ideResourceService;

    @Autowired
    private IdeConfigService ideConfigService;

    @Value("${ide.vscode.image}")
    private String vscodeImage;
    @Value("${ide.sshserver.image}")
    private String sshServerImage;
    @Value("${ide.wetty.image}")
    private String wettyImage;
    @Value("${ide.notebook.image}")
    private String notebookImage;
    @Value("${ide.comdev.pvcName}")
    private String comDevPvc;

    @Value("${ide.user.storageClassName}")
    private String storageClassNameForUser;

    public VscodeConfigReconciler(KubernetesClient client, IdeResourceGenerator ideResourceGenerator) {
        this.client = client;
        this.ideResourceGenerator = ideResourceGenerator;
    }
    /**
     * 이 메소드는 이벤트 소스를 준비하고 등록합니다.
     * Kubernetes의 StatefulSet과 Service 리소스에 대한 이벤트 소스를 생성하고, 이를 통해 VS Code 서버의 상태 변화를 감지합니다.
     *
     * @param context 이벤트 소스 컨텍스트
     * @return 등록된 이벤트 소스의 맵
     */
    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<IdeConfig> context) {
        return EventSourceInitializer.nameEventSources(
                new InformerEventSource<>(InformerConfiguration.from(StatefulSet.class, context).withLabelSelector("himang10-vscode-server").build(), context),
                new InformerEventSource<>(InformerConfiguration.from(Service.class, context).withLabelSelector("himang10-vscode-server").build(), context)
        );

    }

    /**
     * 이 메소드는 주어진 IdeConfig 리소스에 대한 조정 로직을 수행합니다.
     * VS Code 서버에 대한 StatefulSet과 Service를 생성하거나 업데이트합니다.
     *
     * @param resource 조정 대상 IdeConfig 리소스
     * @param context 조정 컨텍스트
     * @return 업데이트 컨트롤 객체, 리소스의 상태를 업데이트하거나 다시 조정을 예약합니다.
     */
    @Override
    public UpdateControl<IdeConfig> reconcile(IdeConfig resource, Context<IdeConfig> context) {
        if (resource == null || resource.getSpec() == null) {
            // Null resource or spec: maybe log and return or throw a specific exception
            return UpdateControl.noUpdate(); // or appropriate action
        }

        if (!resource.getMetadata().getNamespace().equals("kube-pattern")) {
            return UpdateControl.noUpdate();
        }

        String namespace = resource.getMetadata().getNamespace();
        IdeConfigSpec spec = resource.getSpec();
        String wettyBasePath = SVCPathGenerator.generatePath(spec) + IdeCommon.WEBSSH_PATH_NAME;

        // VS Code, webSSH 서비스가 활성인지 비활성인지를 확인합니다.
        Boolean isVscode = spec.getServiceTypes().contains(IdeCommon.VSCODE_SERVICE_TYPE);
        Boolean isGit = spec.getVscode() != null;
        Boolean isWebssh = spec.getServiceTypes().contains(IdeCommon.WEBSSH_SERVICE_TYPE);
        Boolean isNotebook = spec.getServiceTypes().contains(IdeCommon.NOTEBOOK_SERVICE_TYPE);

        /**
         * VS Code 서버를 위한 ServiceAccount, Role, RoleBinding, ClusterRole, ClusterRoleBinding 등을 생성하거나
         * 또는 기존 ServiceAccount를 찾아서 마운트할 수 있도록 합니다.
         */
        Optional<String> serviceAccountName = ideResourceService.createOrUpdateAuthentication(resource, namespace, isWebssh);
        if (serviceAccountName.isEmpty()) {
            return UpdateControl.noUpdate();
        }

        /**
         * Git Secret를 생성합니다.
         */
        Optional<String> secretName = ideResourceService.createOrUpdateSecret(resource, namespace, isVscode, isGit);
        if(secretName.isEmpty()) {
            return UpdateControl.noUpdate();
        }

        /**
         * IdeConfig의 ServiceType에 정의되어 있는 서비스 유형 선택에 따라 container 들을 생성합니다.
         */
        List<Container> containers = new ArrayList<>();
        IdeConfig finalResource = resource;
        spec.getServiceTypes().forEach(serviceType -> {
            switch(serviceType) {
                // VS Code 서버 컨테이너 생성
                case "vscode":
                    containers.add(ideResourceGenerator.vscodeServerContainer(
                            finalResource,
                            "vscodeserver",
                            vscodeImage,
                            IdeCommon.VSCODE_PORT,
                            isVscode, isGit));
                    break;
                    // WebSSH 컨테이너 생성
                case "webssh":
                    containers.add(ideResourceGenerator.wettyContainer(
                            finalResource,
                            "wetty",
                            wettyImage,
                            wettyBasePath,
                            IdeCommon.WETTY_PORT));

                    containers.add(ideResourceGenerator.sshServerContainer(
                            finalResource,
                            "sshserver",
                            sshServerImage,
                            IdeCommon.SSH_PORT,
                            isVscode, isGit));
                    break;
                case "notebook":
                    containers.add(ideResourceGenerator.notbookContainer(
                            finalResource,
                            "jupyter",
                            notebookImage,
                            IdeCommon.NOTEBOOK_PORT));
                default:
                    log.info("serviceType is null or empty");
                    return;
            }
        });

        /**
         * IdeConfig 리소스에 정의된 서비스 유형에 따라 StatefulSet과 Service를 생성합니다.
         */
        Optional<String> statefulsetName = ideResourceService.createOrUpdateStatefulset(
                resource, namespace, containers, comDevPvc, storageClassNameForUser, serviceAccountName.get(), isVscode, isGit, isNotebook);
        if(statefulsetName.isEmpty()) {
            return UpdateControl.noUpdate();
        }

        log.info("success to create statefulset");
        log.info("- statefulsetName: " + statefulsetName.get());
        log.info("- namespace: " + namespace);
        log.info("- serviceAccountName: " + serviceAccountName.get());
        log.info("- service: " + SVCPathGenerator.generateName(spec) + IdeCommon.SERVICE_NAME_POSTFIX);
        log.info("- secret: " + secretName.get());

        resource = UpdateStatusHandler.updateStatus(
                resource,
                "Created StatefulSet and Service",
                true);

        //return UpdateControl.patchStatus(resource).rescheduleAfter(10, TimeUnit.SECONDS);
        return UpdateControl.noUpdate();

    }

    /**
     * 이 메소드는 IdeConfig 리소스의 에러 상태를 업데이트합니다.
     * 조정 중 발생한 예외를 처리하고 리소스의 상태를 업데이트합니다.
     *
     * @param ideConfig 에러 상태를 업데이트할 IdeConfig 리소스
     * @param context 조정 컨텍스트
     * @param e 발생한 예외
     * @return 에러 상태 업데이트 컨트롤 객체
     */
    @Override
    public ErrorStatusUpdateControl<IdeConfig> updateErrorStatus(IdeConfig ideConfig, Context<IdeConfig> context, Exception e) {
        return handleError(ideConfig, e);
    }

    /**
     * 이 메소드는 IdeConfig 리소스의 삭제 로직을 수행합니다.
     * 관련된 StatefulSet과 Service를 Kubernetes 클러스터에서 삭제합니다.
     *
     * @param resource 삭제할 IdeConfig 리소스
     * @param context 조정 컨텍스트
     * @return 삭제 컨트롤 객체
     */
    @Override
    public DeleteControl cleanup(IdeConfig resource, Context<IdeConfig> context) {
        String namespace = resource.getMetadata().getNamespace();
        Boolean isVscode = resource.getSpec().getServiceTypes().contains("vscode");
        Boolean isGit = resource.getSpec().getVscode() != null;
        Boolean isWebssh = resource.getSpec().getServiceTypes().contains("webssh");
        Boolean isWebsshCreate = resource.getSpec().getWebssh().getPermission().getUseType().equals("create");
        try {
            ideResourceService.deleteStatefulset(resource, namespace);
            ideResourceService.deleteService(resource, namespace);
            ideResourceService.deleteRoleBinding(resource, namespace);
            ideResourceService.deleteServiceAccount(resource, namespace);

            if(isVscode && isGit ) {
                ideResourceService.deleteSecret(resource, namespace);
            }

            if(isWebssh && isWebsshCreate) {
                ideResourceService.deleteRoleBinding(resource, namespace);
                ideResourceService.deleteServiceAccount(resource, namespace);
            }

        } catch (IdeResourceDeleteException e) {
            return DeleteControl.defaultDelete();
        }

        return DeleteControl.defaultDelete();
    }

}
