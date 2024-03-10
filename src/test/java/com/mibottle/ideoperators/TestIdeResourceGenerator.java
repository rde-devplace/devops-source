package com.mibottle.ideoperators;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mibottle.ideoperators.customresource.*;
import com.mibottle.ideoperators.reconciler.IdeResourceGenerator;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;

class TestIdeResourceGenerator {

    @InjectMocks
    private IdeResourceGenerator ideResourceGenerator;

    @Mock
    private IdeConfig mockIdeConfig;

    @Mock
    private IdeConfigSpec mockIdeConfigSpec;

    @Mock
    private WebSSH mockWebSSH;

    @Mock
    private Vscode mockVscode;

    @Mock
    private Permission mockPermission;

    @Mock
    private InfrastructureSize mockInfrastructureSize;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        // Mock 객체 설정
        when(mockIdeConfig.getSpec()).thenReturn(mockIdeConfigSpec);
        when(mockIdeConfigSpec.getUserName()).thenReturn("himang10");
        when(mockIdeConfigSpec.getServiceTypes()).thenReturn(Arrays.asList("vscode", "webssh"));
        when(mockIdeConfigSpec.getWebssh()).thenReturn(mockWebSSH);
        when(mockIdeConfigSpec.getVscode()).thenReturn(mockVscode);
        when(mockWebSSH.getPermission()).thenReturn(mockPermission);
        when(mockPermission.getRole()).thenReturn("coder");
        when(mockPermission.getScope()).thenReturn("cluster");
        when(mockPermission.getUseType()).thenReturn("create");
        when(mockIdeConfigSpec.getInfrastructureSize()).thenReturn(mockInfrastructureSize);
        when(mockInfrastructureSize.getCpu()).thenReturn("200m");
        when(mockInfrastructureSize.getMemory()).thenReturn("512Mi");
        when(mockInfrastructureSize.getDisk()).thenReturn("20Gi");
        when(mockIdeConfigSpec.getReplicas()).thenReturn(1);
        // 추가 mock 객체 필드 설정...
    }

    @Test
    void testStatefulSetForIDE() {
        // 테스트에 필요한 데이터 설정
        when(mockIdeConfig.getSpec()).thenReturn(mockIdeConfigSpec);

        // StatefulSet 생성 메서드를 호출
        StatefulSet statefulSet = ideResourceGenerator.statefulSetForIDE(
                mockIdeConfig, "statefulset-name", "service-name", "label-name",
                new ArrayList<>(), "external-pvc", "storage-class", "service-account", true, true ,true);

        // 결과 검증
        assertNotNull(statefulSet, "StatefulSet should not be null");
        assertEquals("statefulset-name", statefulSet.getMetadata().getName(), "StatefulSet name should match");
        // 추가 검증 로직 작성

        // 테스트 결과 출력
        System.out.println("StatefulSet Name: " + statefulSet.getMetadata().getName());
        System.out.println("StatefulSet Labels: " + statefulSet.getMetadata().getLabels());
        System.out.println("StatefulSet Replicas: " + statefulSet.getSpec().getReplicas());
        System.out.println(statefulSet);
    }

    @Test
    void testSshServerContainer() {
        // Container 생성 메서드 호출
        Container container = ideResourceGenerator.sshServerContainer(
                mockIdeConfigSpec, "container-name", "image", 2222, true, true);

        // 결과 검증
        assertNotNull(container, "Container should not be null");
        assertEquals("container-name", container.getName(), "Container name should match");
        // 추가 검증 로직 작성
    }

    // 추가 테스트 메서드를 여기에 작성

}
