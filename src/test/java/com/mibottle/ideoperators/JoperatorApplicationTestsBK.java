package com.mibottle.ideoperators;

import com.mibottle.ideoperators.customresource.*;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@SpringBootTest
class JoperatorApplicationTestsBK {

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


        UpdateControl<IdeConfig> updateControl = vscodeConfigReconciler.reconcile(ideConfig, context);
        System.out.println(updateControl);




        Boolean isVscode = ideConfig.getSpec().getServiceTypes().contains("vscode");
        Boolean isGit = ideConfig.getSpec().getVscode() != null;
        IdeResourceGenerator realIdeResourceGenerator = new IdeResourceGenerator();
        List<Container> containers = new ArrayList<Container>();
        containers.add(realIdeResourceGenerator.vscodeServerContainer(ideConfig.getSpec(), "vscodeserver", "vscode-server:1.0", 8443, isVscode, isGit));
        containers.add(realIdeResourceGenerator.sshServerContainer(ideConfig.getSpec(), "sshserver", "ssh-server:1.0", 2222, isVscode, isGit));
        containers.add(realIdeResourceGenerator.wettyContainer(ideConfig.getSpec(), "wetty", "vscode-server:1.0", "/" + ideConfig.getSpec().getUserName() + "/cli/wetty", 3000, isVscode, isGit));

        StatefulSet generatedStatefulSet = realIdeResourceGenerator.statefulSetForIDE(
                ideConfig,
                "vscodeserver-statefulset",
                "vscode-server-serivce",
                "vscode-server", containers,
                "com-dev-pvc",
                "user-dev-storage",
                "com-admin-sa",
                isVscode,
                isGit
        );


        String yamlRepresentation = Yaml.dump(generatedStatefulSet);
        System.out.println(yamlRepresentation);

        /*
        // Specify the path to save the file
        Path outputPath = Paths.get("./generatedStatefulset.yaml");
        try {
            Files.write(outputPath, yamlRepresentation.getBytes());
            System.out.println("YAML file saved to: " + outputPath.toString());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error saving the YAML file.");
        }

         */

        assertNotNull(generatedStatefulSet);
        assertEquals("vscodeserver-statefulset", generatedStatefulSet.getMetadata().getName());

        // When
       // controller.reconcile(ideConfig, context);

        // Then
        //verify(client.apps().statefulSets().inNamespace(any()).resource(any()), times(1)).create();
        //verify(client.services().inNamespace(any()).resource(any()), times(1)).create();
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
