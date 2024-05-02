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
import com.mibottle.ideoperators.model.IdeCommon;
import com.mibottle.ideoperators.util.HashUtil;
import com.mibottle.ideoperators.util.SVCPathGenerator;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.rbac.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
/**
 * ResourceGenerator는 Kubernetes 내에서 IDE 관련 리소스를 생성하고 관리하는 클래스입니다.
 * 이 클래스는 주로 StatefulSet과 Service 리소스를 생성하는 데 사용됩니다.
 */
public class IdeResourceGenerator {

    /**
     * IDE에 대한 StatefulSet을 생성합니다.
     *
     * @param resource IDE 설정을 담고 있는 IdeConfig 객체
     * @param statefulSetName 생성할 StatefulSet의 이름
     * @param serviceName StatefulSet에 연결될 Service의 이름
     * @param labelName 리소스에 적용될 레이블 이름
     * @param containers 컨테이너 목록 (예: vscodeserver, wetty, ssh)
     * @param externalComDevPVC 공유 PVC의 이름
     * @param storageClassNameForUser 사용자 데이터 저장을 위한 StorageClass 이름
     * @return 생성된 StatefulSet 객체
     */
    public StatefulSet statefulSetForIDE(
            IdeConfig resource,             // IdeConfig Custom Resource
            String statefulSetName,         // Statefulset 이름
            String serviceName,             // Statefulset을 외부에 노출 시 사용하는 ServiceName
            String labelName,               // Match Label 등을 위한 LabelName
            List<Container> containers,     // vscodeserver, wetty, ssh
            String externalComDevPVC,       // 외부 공통 Extension 등의 리소스 공유 PVC
            String storageClassNameForUser,  // 내부 사용자 Disk 자원 할당 (Block Storage)
            String serviceAccountName,      // IDE에 대한 ServiceAccount
            Boolean isVscode,
            Boolean isGit,
            Boolean isNotebook
    ) {
        IdeConfigSpec spec = resource.getSpec();
        List<Volume> volumes = new ArrayList<>();
        List<PersistentVolumeClaim> volumeClaims = new ArrayList<>();


        // IDE를 초기화하기 위한 공통 PVC를 추가합니다.
        volumes.add (new VolumeBuilder()
                .withName(IdeCommon.INIT_COMM_STORAGE)
                .withNewPersistentVolumeClaim()
                .withClaimName(externalComDevPVC) // com-dev-pvc
                .endPersistentVolumeClaim()
                .build());

        // Web IDE를 사용하는 경우에만 Git Sercret 과 개발 환경을 위한 PVC를 추가한다.
        if(isVscode || isNotebook) {
            // IDE를 위한 개별 개발 환경을 볼륨 클레임을 구성한다
            volumeClaims.add(new PersistentVolumeClaimBuilder()
                    .withNewMetadata()
                    .withName(IdeCommon.USER_DEV_STORAGE)
                    .endMetadata()
                    .withNewSpec()
                    .withAccessModes(IdeCommon.USER_DEV_STORAGE_ACCESS_MODE)
                    .withStorageClassName(storageClassNameForUser)
                    .withNewResources()
                    .addToRequests("storage", new Quantity(spec.getInfrastructureSize().getDisk()))
                    .endResources()
                    .endSpec()
                    .build());
        }
        if(isGit) {
                String name_prefix = SVCPathGenerator.generateName(spec);
                String secretName = name_prefix + IdeCommon.SECRET_NAME_POSTFIX;
                // IDE를 위한 Secret을 추가합니다. (Git 정보)
                volumes.add(new VolumeBuilder()
                        .withName(secretName)
                        .withNewSecret()
                        .withSecretName(secretName) // 여기에 Secret 이름 지정
                        .endSecret()
                        .build());
        }


        return new StatefulSetBuilder()
                .withNewMetadata()
                .withName(statefulSetName)
                .withAnnotations(Map.of(IdeCommon.IDECONFIG_GROUP, IdeCommon.IDECONFIG_CRD_PLURAL, "userName", resource.getSpec().getUserName()))
                .withLabels(Map.of("app", labelName, IdeCommon.OPERATOR_LABEL_KEY, IdeCommon.OPERATOR_LABEL_VALUE, IdeCommon.OPERATOR_TYPE_LABEL_KEY, IdeCommon.OPERATOR_TYPE_LABEL_VALUE))
                .endMetadata()
                .withNewSpec()
                .withReplicas(spec.getReplicas())
                .withNewSelector()
                .addToMatchLabels(IdeCommon.OPERATOR_LABEL_KEY, IdeCommon.OPERATOR_LABEL_VALUE)
                .addToMatchLabels(IdeCommon.OPERATOR_TYPE_LABEL_KEY, IdeCommon.OPERATOR_TYPE_LABEL_VALUE)
                .addToMatchLabels("app", labelName)
                .endSelector()
                // ... 기타 필요한 설 정
                .withServiceName(serviceName)
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(Map.of("app", labelName, IdeCommon.OPERATOR_LABEL_KEY, IdeCommon.OPERATOR_LABEL_VALUE, IdeCommon.OPERATOR_TYPE_LABEL_KEY, IdeCommon.OPERATOR_TYPE_LABEL_VALUE))
                //.withAnnotations(Map.of("update", HashUtil.generateSHA256Hash(spec)))
                .endMetadata()
                //--- Container Spec 설정
                .withNewSpec()
                .withServiceAccountName(serviceAccountName)

                //container 리소스 설정 (vscodeserver, wetty, sshserver)
                .withContainers(containers)
                .withVolumes(volumes)
                .endSpec()
                .endTemplate()
                .withVolumeClaimTemplates(volumeClaims)
                .endSpec()
                .build();

    }


    /**
     * SSH 서버 컨테이너를 생성합니다.
     *
     * @param spec IdeConfigSpec 객체
     * @param containerName 컨테이너의 이름
     * @param image 컨테이너 이미지
     * @param port 컨테이너 포트
     * @return 생성된 Container 객체
     */
    public Container sshServerContainer(IdeConfig resource, String containerName, String image, Integer port, Boolean isVscode, Boolean isGit) {
        // Create  container
        ContainerBuilder containerBuilder = new ContainerBuilder()
                .withName(containerName)
                .withImage(image)
                .withImagePullPolicy("Always")

                // ... Command 설정
                .withCommand("/bin/sh", "-c", "/usr/local/bin/configure-kubeconfig && /start.sh && /init")
                // ... Port 설정
                .withPorts(new ContainerPortBuilder()
                        .withContainerPort(port) //2222
                        .build());


        // ... ENV 설정
        containerBuilder.addNewEnv()
                .withName("USER_NAME")
                .withValue("linuxserver.io")
                .endEnv()
                .addNewEnv()
                .withName("PASSWORD_ACCESS")
                .withValue("true")
                .endEnv()
                .addNewEnv()
                .withName("SUDO_ACCESS")
                .withValue("true")
                .endEnv()
                .addNewEnv()
                .withName("USER_PASSWORD")
                .withValue("password")
                .endEnv();

                // ... Volume Mount 설정
        if (isVscode) {
            containerBuilder.addNewVolumeMount()
                    .withName("user-dev-storage")
                    .withMountPath("/config")
                    .endVolumeMount()
                    .addNewVolumeMount()
                    .withName("com-dev-storage")
                    .withMountPath("/common-config")
                    .endVolumeMount();
        }


                return containerBuilder.build();
    }

    /**
     * VSCode 서버 컨테이너를 생성합니다.
     *
     * @param spec IdeConfigSpec 객체
     * @param containerName 컨테이너의 이름
     * @param image 컨테이너 이미지
     * @param port 컨테이너 포트
     * @return 생성된 Container 객체
     */
    public Container vscodeServerContainer(IdeConfig resource, String containerName, String image, Integer port, Boolean isVscode, Boolean isGit, String ideProxyDomain, String appDomainType) {
        IdeConfigSpec spec = resource.getSpec();

        ContainerBuilder containerBuilder = new ContainerBuilder()
                .withName(containerName)
                .withImage(image)
                .withImagePullPolicy("Always")

                // ... Resource 설정
                .withResources(new ResourceRequirementsBuilder()
                        .addToRequests("cpu", new Quantity(resource.getSpec().getInfrastructureSize().getCpu()))
                        .addToRequests("memory", new Quantity(resource.getSpec().getInfrastructureSize().getMemory()))
                        .addToLimits("cpu", new Quantity(resource.getSpec().getInfrastructureSize().getCpu()))
                        .addToLimits("memory", new Quantity(resource.getSpec().getInfrastructureSize().getMemory()))
                        .build())

                .withPorts(new ContainerPortBuilder()
                        .withContainerPort(port)  //8443
                        .build())

                // ... startup probe 설정
                .withNewStartupProbe()
                .withHttpGet(new HTTPGetActionBuilder()
                        .withPath("/vscode/")
                        .withNewPort(port)
                        .build())
                .withFailureThreshold(30)
                .withPeriodSeconds(10)
                .endStartupProbe()
                .withNewLivenessProbe()
                .withHttpGet(new HTTPGetActionBuilder()
                        .withPath("/vscode/")
                        .withNewPort(port)
                        .build())
                .withInitialDelaySeconds(5)
                .withFailureThreshold(3)
                .withPeriodSeconds(30)
                .endLivenessProbe()
                // ... Volume Mount 설정
                .addNewVolumeMount()
                .withName("user-dev-storage")
                .withMountPath("/config")
                .endVolumeMount()
                .addNewVolumeMount()
                .withName("com-dev-storage")
                .withMountPath("/common-config")
                .withReadOnly(true)
                .endVolumeMount();

        // anntation의 packageType.cloriver.io/vscode: "basic"
        // 이것은 code-init의 extensions/install-${PACKAGETYPE}.yaml 파일을 사용하여 설치 대상을 식별한다. (예: basic, python, java, nodejs, go, csharp)
        String packageType = resource.getMetadata().getAnnotations().get(IdeCommon.PACKAGE_TYPE);
        if (packageType != null) {
            containerBuilder.addNewEnv()
                    .withName("PACKAGETYPE")
                    .withValue(packageType)
                    .endEnv();
        }
        String domainProxy = resource.getMetadata().getAnnotations().get(IdeCommon.PROXY_DOMAIN);
        String svcName = SVCPathGenerator.generateName(spec);

        // IDE Proxy Domain이 없는 경우에는 ideProxyDomain을 사용한다.
        if (domainProxy == null) domainProxy = ideProxyDomain;
        // IDE Proxy Domain이 path인 경우에는 App Path는 /himan10/8501 처럼 Path 사용
        log.debug("vscodeServerContainer appDomainType: {}", appDomainType);
        if(appDomainType.equals("path")) {
            containerBuilder.addNewEnv()
                    .withName("VSCODE_PROXY_URI")
                    .withValue("https://" + domainProxy +"/" + svcName + "/" + "{{port}}")
                    .endEnv();
        } else {
            containerBuilder.addNewEnv()
                    .withName("VSCODE_PROXY_URI")
                    .withValue("https://" + svcName + "p" + "{{port}}." + domainProxy)
                    .endEnv();
        }

        if(isGit) {
            String name_prefix = SVCPathGenerator.generateName(spec);
            String secretName = name_prefix + IdeCommon.SECRET_NAME_POSTFIX;
            containerBuilder.addNewVolumeMount()
                    .withName(secretName)
                    .withMountPath("/etc/git-secret")
                    .withReadOnly(true)
                    .endVolumeMount();

            // git secret 을 환경 변수로 주입한다
            containerBuilder
                    .addNewEnv()
                    .withName("SUDO_PASSWORD")
                    .withValue("password")
                    .endEnv()
                    // git id를 "GIT_ID" 환경 변수로 매핑
                    .addNewEnv()
                    .withName(IdeCommon.GIT_ID.toUpperCase())
                    .withNewValueFrom()
                    .withNewSecretKeyRef()
                    .withName(secretName)
                    .withKey(IdeCommon.GIT_ID)
                    .endSecretKeyRef()
                    .endValueFrom()
                    .endEnv()
                    // git token을 "GIT_TOKEN" 환경 변수로 매핑
                    .addNewEnv()
                    .withName(IdeCommon.GIT_TOKEN.toUpperCase())
                    .withNewValueFrom()
                    .withNewSecretKeyRef()
                    .withName(secretName)
                    .withKey(IdeCommon.GIT_TOKEN)
                    .endSecretKeyRef()
                    .endValueFrom()
                    .endEnv()
                    // git repository를 "GIT_REPOSITORY" 환경 변수로 매핑
                    .addNewEnv()
                    .withName(IdeCommon.GIT_REPOSITORY.toUpperCase())
                    .withNewValueFrom()
                    .withNewSecretKeyRef()
                    .withName(secretName)
                    .withKey(IdeCommon.GIT_REPOSITORY)
                    .endSecretKeyRef()
                    .endValueFrom()
                    .endEnv()
                    // git branch를 "GIT_BRANCH" 환경 변수로 매핑
                    .addNewEnv()
                    .withName(IdeCommon.GIT_BRANCH.toUpperCase())
                    .withNewValueFrom()
                    .withNewSecretKeyRef()
                    .withName(secretName)
                    .withKey(IdeCommon.GIT_BRANCH)
                    .endSecretKeyRef()
                    .endValueFrom()
                    .endEnv();

        }

        return containerBuilder.build();
    }

    /**
     * Wetty 컨테이너를 생성합니다.
     *
     * @param spec IdeConfigSpec 객체
     * @param containerName 컨테이너의 이름
     * @param image 컨테이너 이미지
     * @param basePath 기본 경로
     * @param port 컨테이너 포트
     * @return 생성된 Container 객체
     */
    public Container wettyContainer(IdeConfig resource, String containerName, String image, String basePath, Integer port) {
        // Create  container
        return new ContainerBuilder()
                .withName(containerName)
                .withImage(image)
                .withImagePullPolicy("Always")

                // ... Port 설정
                .withPorts(new ContainerPortBuilder()
                        .withContainerPort(port)  //3000
                        .build())

                // ... ENV 설정
                .addNewEnv()
                .withName("BASE")
                .withValue(basePath)
                .endEnv()
                .addNewEnv()
                .withName("SSHHOST")
                .withValue("127.0.0.1")
                .endEnv()
                .addNewEnv()
                .withName("SSHPORT")
                .withValue("2222")
                .endEnv()
                .addNewEnv()
                .withName("SSHUSER")
                .withValue("linuxserver.io")
                .endEnv()
                .addNewEnv()
                .withName("SSHPASS")
                .withValue("password")
                .endEnv()
                .addNewEnv()
                .withName("COMMAND")
                .withValue("/bin/zsh")
                .endEnv()
                .build();
    }


    /**
     * Jupyter Notebook을 생성합니다.
     *
     * @param spec IdeConfigSpec 객체
     * @param containerName 컨테이너의 이름
     * @param image 컨테이너 이미지
     * @param port 컨테이너 포트
     * @return 생성된 Container 객체
     */
    public Container notbookContainer(IdeConfig resource, String containerName, String image, Integer port) {

        // Create  container
        ContainerBuilder containerBuilder = new ContainerBuilder()
                .withName(containerName)
                .withImage(image)
                .withImagePullPolicy("Always")

                // ... Resource 설정
                .withResources(new ResourceRequirementsBuilder()
                        .addToRequests("cpu", new Quantity(resource.getSpec().getInfrastructureSize().getCpu()))
                        .addToRequests("memory", new Quantity(resource.getSpec().getInfrastructureSize().getMemory()))
                        .addToLimits("cpu", new Quantity(resource.getSpec().getInfrastructureSize().getCpu()))
                        .addToLimits("memory", new Quantity(resource.getSpec().getInfrastructureSize().getMemory()))
                        .build())

                // ... Port 설정
                .withPorts(new ContainerPortBuilder()
                        .withContainerPort(port) //8888
                        .build());


        // ... ENV 설정
        containerBuilder.addNewEnv()
                .withName("JUPYTER_ENABLE_LAB")
                .withValue("yes")
                .endEnv()
                .addNewEnv()
                .withName("JUPYTER_TOKEN")
                .withValue("")
                .endEnv()
                .addNewEnv()
                .withName("NOTEBOOK_BASE_PATH")
                .withValue(SVCPathGenerator.generatePath(resource.getSpec()) + "/jupyter")
                .endEnv()
                .addNewEnv()
                .withName("NOTEBOOK_PORT")
                .withValue(IdeCommon.NOTEBOOK_PORT.toString())
                .endEnv()
                .addNewEnv()
                .withName("STREAMLIT_SERVER_ENABLE_XSRF_PROTECTION")  // XSRF 보호 기능 비활성화
                .withValue("false")
                .endEnv()
                .addNewEnv()
                .withName("STREAMLIT_SERVER_ENABLE_CORS") // CORS 비활성화
                .withValue("false")
                .endEnv()
        ;

        // ... Volume Mount 설정
        containerBuilder
                .addNewVolumeMount()
                .withName("user-dev-storage")
                .withMountPath("/config")
                .endVolumeMount();

        return containerBuilder.build();
    }



    /**
     * IDE에 대한 Kubernetes Service를 생성합니다.
     *
     * @param resource IDE 설정을 담고 있는 IdeConfig 객체
     * @param serviceName 생성할 Service의 이름
     * @param labelName 리소스에 적용될 레이블 이름
     * @return 생성된 Service 객체
     */
    public io.fabric8.kubernetes.api.model.Service serviceForIDE(IdeConfig resource, String serviceName, String labelName) {
        List<ServicePort> servicePorts = resource.getSpec().getPortList().stream()
                .map(port -> new ServicePort("TCP", port.getName(), null, port.getPort(), port.getProtocol(), new IntOrString(port.getTargetPort())))
                .collect(Collectors.toList());

        /**
         * vscode-server, wetty, ssh-server에 대한 port를 등록하낟
         */
        servicePorts.add(new ServicePort("TCP", "vscode", null, IdeCommon.VSCODE_PORT, "TCP", new IntOrString(IdeCommon.VSCODE_PORT)));
        servicePorts.add(new ServicePort("TCP", "ssh", null, IdeCommon.SSH_PORT, "TCP", new IntOrString(IdeCommon.SSH_PORT)));
        servicePorts.add(new ServicePort("TCP", "wetty", null, IdeCommon.WETTY_PORT, "TCP", new IntOrString(IdeCommon.WETTY_PORT)));
        servicePorts.add(new ServicePort("TCP", "jupyter", null, IdeCommon.NOTEBOOK_PORT, "TCP", new IntOrString(IdeCommon.NOTEBOOK_PORT)));

        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(serviceName)
                .withAnnotations(Map.of(IdeCommon.IDECONFIG_GROUP, IdeCommon.IDECONFIG_CRD_PLURAL, "userName", resource.getSpec().getUserName()))
                .addToLabels(IdeCommon.OPERATOR_LABEL_KEY, IdeCommon.OPERATOR_LABEL_VALUE)
                .addToLabels(IdeCommon.OPERATOR_TYPE_LABEL_KEY, IdeCommon.OPERATOR_TYPE_LABEL_VALUE)
                .addToLabels("app", labelName)
                .endMetadata()
                .withNewSpec()
                .withSelector(Map.of("app", labelName, IdeCommon.OPERATOR_LABEL_KEY, IdeCommon.OPERATOR_LABEL_VALUE, IdeCommon.OPERATOR_TYPE_LABEL_KEY, IdeCommon.OPERATOR_TYPE_LABEL_VALUE))
                .withPorts(servicePorts)
                .endSpec()
                .build();

        return service;
    }

    /**
     * Web IDE를 위한 Git Secret을 생성합니다.
     * @param resource
     * @param secretName
     * @param labelName
     * @return
     */
    public Secret secretForIDE(IdeConfig resource, String secretName, String labelName) {
        String gitId = Base64.getEncoder().encodeToString(resource.getSpec().getVscode().getGit().getId().getBytes());
        String gitRepository = Base64.getEncoder().encodeToString(resource.getSpec().getVscode().getGit().getRepository().getBytes());

        SecretBuilder secretBuilder = new SecretBuilder()
                .withNewMetadata()
                .withName(secretName)
                .withAnnotations(Map.of(IdeCommon.IDECONFIG_GROUP, IdeCommon.IDECONFIG_CRD_PLURAL, "userName", resource.getSpec().getUserName()))
                .addToLabels(IdeCommon.OPERATOR_LABEL_KEY, IdeCommon.OPERATOR_LABEL_VALUE)
                .addToLabels(IdeCommon.OPERATOR_TYPE_LABEL_KEY, IdeCommon.OPERATOR_TYPE_LABEL_VALUE)
                .addToLabels("app", labelName)
                .endMetadata()
                .withType("Opaque")
                .addToData(IdeCommon.GIT_ID, gitId)
                .addToData(IdeCommon.GIT_REPOSITORY, gitRepository);

        // token 필드가 존재하는 경우에만 추가
        if (resource.getSpec().getVscode().getGit().getToken() != null) {
            String gitToken = Base64.getEncoder().encodeToString(resource.getSpec().getVscode().getGit().getToken().getBytes());
            secretBuilder.addToData(IdeCommon.GIT_TOKEN, gitToken);
        } else
            secretBuilder.addToData(IdeCommon.GIT_TOKEN, "");

        // branch 필드가 존재하는 경우에만 추가
        if (resource.getSpec().getVscode().getGit().getBranch() != null) {
            String gitBranch = Base64.getEncoder().encodeToString(resource.getSpec().getVscode().getGit().getBranch().getBytes());
            secretBuilder.addToData(IdeCommon.GIT_BRANCH, gitBranch);
        } else
            secretBuilder.addToData(IdeCommon.GIT_BRANCH, "");

        return secretBuilder.build();
    }

    /**
     * WebSSH를 위한 ServiceAccount를 생성합니다.
     * @param resource
     * @param serviceAccountName
     * @param labelName
     * @return
     */
    public ServiceAccount serviceAccountForIDE(IdeConfig resource, String serviceAccountName, String labelName) {
        if (    resource.getSpec().getWebssh().getPermission().getUseType().equals("use") &&
                resource.getSpec().getWebssh().getPermission().getServiceAccountName() != null) {
            serviceAccountName = resource.getSpec().getWebssh().getPermission().getServiceAccountName();
        }

        ServiceAccount serviceAccount = new ServiceAccountBuilder()
                .withNewMetadata()
                .withName(serviceAccountName)
                .withAnnotations(Map.of(IdeCommon.IDECONFIG_GROUP, IdeCommon.IDECONFIG_CRD_PLURAL, "userName", resource.getSpec().getUserName()))
                .addToLabels(IdeCommon.OPERATOR_LABEL_KEY, IdeCommon.OPERATOR_LABEL_VALUE)
                .addToLabels(IdeCommon.OPERATOR_TYPE_LABEL_KEY, IdeCommon.OPERATOR_TYPE_LABEL_VALUE)
                .addToLabels("app", labelName)
                .endMetadata()
                .build();

        return serviceAccount;
    }

    /**
     * WebSSH에서 사용하기 위한 RoleBinding을 생성합니다.
     * @param resource
     * @param ideRole
     * @param roleBindingName
     * @param labelName
     * @param serviceAccountName
     * @return
     */
    public RoleBinding roleBindingForIDE(IdeConfig resource, IdeRole ideRole, String roleBindingName, String labelName, String serviceAccountName) {
        RoleBinding roleBinding = new RoleBindingBuilder()
                .withNewMetadata()
                .withName(roleBindingName)
                .withAnnotations(Map.of(IdeCommon.IDECONFIG_GROUP, IdeCommon.IDECONFIG_CRD_PLURAL, "userName", resource.getSpec().getUserName()))
                .addToLabels(IdeCommon.OPERATOR_LABEL_KEY, IdeCommon.OPERATOR_LABEL_VALUE)
                .addToLabels(IdeCommon.OPERATOR_TYPE_LABEL_KEY, IdeCommon.OPERATOR_TYPE_LABEL_VALUE)
                .addToLabels("app", labelName)
                .endMetadata()
                .withNewRoleRef()
                .withKind(ideRole.getKind())
                .withName(ideRole.getName())
                .withApiGroup("rbac.authorization.k8s.io")
                .endRoleRef()
                .addNewSubject()
                .withKind("ServiceAccount")
                .withName(serviceAccountName)
                .endSubject()
                .build();

        return roleBinding;
    }

    /**
     * WebSSH에서 사용하기 위한 ClusterRoleBinding을 생성합니다.
     * @param resource
     * @param ideRole
     * @param clusterRoleBindingName
     * @param labelName
     * @param serviceAccountName
     * @return
     */
    public ClusterRoleBinding clusterRoleBindingForIDE(IdeConfig resource, IdeRole ideRole, String clusterRoleBindingName, String labelName, String serviceAccountName) {
        ClusterRoleBinding roleBinding = new ClusterRoleBindingBuilder()
                .withNewMetadata()
                .withName(clusterRoleBindingName)
                .withAnnotations(Map.of(IdeCommon.IDECONFIG_GROUP, IdeCommon.IDECONFIG_CRD_PLURAL, "userName", resource.getSpec().getUserName()))
                .addToLabels(IdeCommon.OPERATOR_LABEL_KEY, IdeCommon.OPERATOR_LABEL_VALUE)
                .addToLabels(IdeCommon.OPERATOR_TYPE_LABEL_KEY, IdeCommon.OPERATOR_TYPE_LABEL_VALUE)
                .addToLabels("app", labelName)
                .endMetadata()
                .withNewRoleRef()
                .withKind(ideRole.getKind())
                .withName(ideRole.getName())
                .withApiGroup("rbac.authorization.k8s.io")
                .endRoleRef()
                .addNewSubject()
                .withKind("ServiceAccount")
                .withName(serviceAccountName)
                .withNamespace(resource.getMetadata().getNamespace())
                .endSubject()
                .build();

        return roleBinding;
    }


    /**
     * 사용자 권한에 따라 IDE에 대한 Role을 생성합니다.
     * @param resource
     * @return
     */
    public IdeRole getIdeRole(IdeConfig resource, String clusterRoleAdmin, String clusterRoleView) {
        String roleBinding = "ClusterRoleBinding";
        if (resource.getSpec().getWebssh().getPermission().getScope().equals(IdeCommon.WEBSSH_PERMISSION_SCOPE_NAMESPACE))
            roleBinding = "RoleBinding";
        switch(resource.getSpec().getWebssh().getPermission().getRole()) {
            case "administrator":
                return new IdeRole("ClusterRole", clusterRoleAdmin, roleBinding);
                //return new IdeRole("ClusterRole", "view", "ClusterRoleBinding");
            case "architect":
                return new IdeRole("ClusterRole", clusterRoleAdmin, roleBinding);
            case "developer":
                return new IdeRole("ClusterRole", clusterRoleAdmin, roleBinding);
            case "coder":
                return new IdeRole("ClusterRole", clusterRoleView, roleBinding);
        }
        return new IdeRole("ClusterRole", clusterRoleView, "RoleBinding");
    }
}
