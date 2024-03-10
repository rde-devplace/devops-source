package com.mibottle.ideoperators;

import com.mibottle.ideoperators.customresource.*;
import com.mibottle.ideoperators.model.IdeCommon;
import com.mibottle.ideoperators.reconciler.IdeResourceGenerator;
import com.mibottle.ideoperators.reconciler.VscodeConfigReconciler;
import com.mibottle.ideoperators.service.IdeResourceService;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@SpringBootTest
class JoperatorApplicationTests {

    @MockBean
    private KubernetesClient client;

    @MockBean
    private IdeResourceGenerator ideResourceGenerator;

    @MockBean
    private IdeResourceService ideResourceService;

    @MockBean
    private VscodeConfigReconciler vscodeConfigReconciler;

    @Autowired
    private VscodeConfigReconciler controller;

    @Test
    public void testStatefulset() {
        // Given
        IdeConfig ideConfig = new IdeConfig(); // initialize with appropriate values
        IdeConfigSpec ideConfigSpec = new IdeConfigSpec();
        ideConfigSpec.setReplicas(1);
        InfrastructureSize is = new InfrastructureSize();
        is.setDisk("500Gi");
        is.setCpu("1000m");
        is.setMemory("1Gi");

        Permission pms = new Permission();
        pms.setRole("com-admin-role");
        pms.setUseType("create");
        pms.setScope("cluster");

        Git git = new Git();
        git.setId("himang10");
        git.setRepository("https://github.com/himang10/kubernetes-pattern.git");
        git.setToken("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");

        WebSSH webSSH = new WebSSH();
        webSSH.setPermission(pms);

        Vscode vscode = new Vscode();
        vscode.setGit(git);

        ideConfig.setSpec(ideConfigSpec);
        ideConfigSpec.setInfrastructureSize(is);
        ideConfigSpec.setWebssh(webSSH);
        ideConfigSpec.setVscode(vscode);
        ideConfig.getSpec().setUserName("testuser");
        ideConfig.getSpec().setServiceTypes(List.of("vscode", "webssh"));

        ideConfigSpec.setReplicas(1);
        ideConfig.getSpec().setPortList(List.of(new Port("tcp", "TCP", 8080, 8080), new Port("ssh", "TCP", 2222, 2222), new Port("wetty", "TCP", 3000, 3000)));
        Context<IdeConfig> context = mock(Context.class); // or initialize with appropriate values
        String vscodeImage = new String("amdp-registry.skamdp.org/mydev-ywyi/amdp-vscode-server:1.0");
        String sshServerImage = new String("amdp-registry.skamdp.org/mydev-ywyi/ssh-with-k9s-extend:1.0");
        String wettyImage = new String("amdp-registry.skamdp.org/mydev-ywyi/wetty-with-k9s:1.1");
        String comDevPvc = new String("com-dev-pvc");
        String storageClassNameForUser = new String("gp2");



        IdeConfig resource = ideConfig;

        if (resource == null || resource.getSpec() == null) {
            // Null resource or spec: maybe log and return or throw a specific exception
            return; // or appropriate action
        }

        String namespace = resource.getMetadata().getNamespace();
        IdeConfigSpec spec = resource.getSpec();
        String wettyBasePath = "/" + spec.getUserName() + "/cli";

        // VS Code, webSSH 서비스가 활성인지 비활성인지를 확인합니다.
        Boolean isVscode = spec.getServiceTypes().contains("vscode");
        Boolean isGit = spec.getVscode() != null;
        Boolean isWebssh = spec.getServiceTypes().contains("webssh");
        Boolean isNotebook = spec.getServiceTypes().contains("notebook");

        /**
         * VS Code 서버를 위한 ServiceAccount, Role, RoleBinding, ClusterRole, ClusterRoleBinding 등을 생성하거나
         * 또는 기존 ServiceAccount를 찾아서 마운트할 수 있도록 합니다.
         */
        Optional<String> serviceAccountName = ideResourceService.createOrUpdateAuthentication(resource, namespace, isWebssh);
        if (serviceAccountName.isEmpty()) {
            return;
        }

        /**
         * Git Secret를 생성합니다.
         */
        Optional<String> secretName = ideResourceService.createOrUpdateSecret(resource, namespace, isVscode, isGit);
        if(secretName.isEmpty()) {
            return;
        }

        /**
         * IdeConfig의 ServiceType에 정의되어 있는 서비스 유형 선택에 따라 container 들을 생성합니다.
         */
        List<Container> containers = new ArrayList<>();
        spec.getServiceTypes().forEach(serviceType -> {
            switch(serviceType) {
                // VS Code 서버 컨테이너 생성
                case "vscode":
                    containers.add(ideResourceGenerator.vscodeServerContainer(spec, "vscodeserver", vscodeImage, 8443, isVscode, isGit));
                    break;
                // WebSSH 컨테이너 생성
                case "webssh":
                    containers.add(ideResourceGenerator.wettyContainer(spec,"wetty", wettyImage, wettyBasePath, 3000));
                    containers.add(ideResourceGenerator.sshServerContainer(spec,"sshserver", sshServerImage, 2222, isVscode, isGit));
                    break;
                default:
                    System.out.println("serviceType is null or empty");
                    return;
            }
        });

        /**
         * IdeConfig 리소스에 정의된 서비스 유형에 따라 StatefulSet과 Service를 생성합니다.
         */
        Optional<String> statefulsetName = ideResourceService.createOrUpdateStatefulset(
                resource, namespace, containers, comDevPvc, storageClassNameForUser, serviceAccountName.get(), isVscode, isGit, isNotebook);
        if(statefulsetName.isEmpty()) {
            return;
        }

        System.out.println("success to create statefulset");
        System.out.println("- statefulsetName: " + statefulsetName.get());
        System.out.println("- namespace: " + namespace);
        System.out.println("- serviceAccountName: " + serviceAccountName.get());
        System.out.println("- service: " + spec.getUserName() + IdeCommon.SERVICE_NAME_POSTFIX);
        System.out.println("- secret: " + secretName.get());


    }

    @Test
    public void testService() {
        // Given
        IdeConfig ideConfig = new IdeConfig(); // initialize with appropriate values
        IdeConfigSpec ideConfigSpec = new IdeConfigSpec();
        List<Port> portList = new ArrayList<>();
        portList.add(new Port("tcp", "TCP", 8080, 8080));
        portList.add(new Port("ssh", "TCP", 2222, 2222));
        portList.add(new Port("wetty", "TCP", 3000, 3000));

        ideConfigSpec.setPortList(portList);
        ideConfig.setSpec(ideConfigSpec);
        Context<IdeConfig> context = mock(Context.class); // or initialize with appropriate values

        IdeResourceGenerator realIdeResourceGenerator = new IdeResourceGenerator();

        Service generatedService = realIdeResourceGenerator.serviceForIDE(
                ideConfig,
                "vscodeserver-service",
                "vscode-server");


        String yamlRepresentation = Yaml.dump(generatedService);
        System.out.println(yamlRepresentation);

        assertNotNull(generatedService);
        assertEquals("vscodeserver-service", generatedService.getMetadata().getName());
    }

}
