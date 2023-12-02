package com.mibottle.ideoperators.reconciler;

import com.mibottle.ideoperators.customresource.IdeConfig;
import com.mibottle.ideoperators.customresource.IdeConfigSpec;
import com.mibottle.ideoperators.util.HashUtil;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ResourceGenerator는 Kubernetes 내에서 IDE 관련 리소스를 생성하고 관리하는 클래스입니다.
 * 이 클래스는 주로 StatefulSet과 Service 리소스를 생성하는 데 사용됩니다.
 */
public class IdeSourceGeneratorForYaml {


    /**
     * YAML 파일에서 StatefulSet을 로드하고 수정합니다.
     *
     * @param client KubernetesClient 객체
     * @param resource IdeConfig 객체
     * @param path YAML 파일 경로
     * @param statefulSetName 생성할 StatefulSet의 이름
     * @param labelName 리소스에 적용될 레이블 이름
     * @return 수정된 StatefulSet 객체
     */
    public StatefulSet statefulSetFromYaml(
            KubernetesClient client,
            IdeConfig resource,
            String path,
            String statefulSetName,
            String labelName) {
        StatefulSet statefulSet = client.apps().statefulSets().load(path).get();


        statefulSet.getMetadata().setName(statefulSetName);
        statefulSet.getSpec().getSelector().getMatchLabels()
                .replace("app", labelName);
        statefulSet.getSpec().getTemplate().getMetadata().getLabels()
                .replace("app", labelName);
        statefulSet.getSpec().getTemplate().getMetadata().getAnnotations()
                .replace("update", HashUtil.generateSHA256Hash(resource.getSpec()));
        statefulSet.getSpec().getVolumeClaimTemplates().get(0).getSpec().getResources().getRequests()
                .replace("storage", new Quantity(resource.getSpec().getInfrastructureSize().getDisk()));

        return statefulSet;
    }


    /**
     * YAML 파일에서 Service를 로드하고 수정합니다.
     *
     * @param client KubernetesClient 객체
     * @param resource IdeConfig 객체
     * @param path YAML 파일 경로
     * @param serviceName 생성할 Service의 이름
     * @param labelName 리소스에 적용될 레이블 이름
     * @return 수정된 Service 객체
     */
    public Service serviceFromYaml(
            KubernetesClient client,
            IdeConfig resource,
            String path,
            String serviceName,
            String labelName) {
        Service service = client.services().load(path).get();
        service.getMetadata().setName(serviceName);
        service.getMetadata().getLabels().replace("app", labelName);
        service.getSpec().getSelector().replace("app", labelName);
        service.getSpec().getPorts().forEach(port -> {
            resource.getSpec().getPortList().forEach(portSpec -> {
                if (port.getName().equals(portSpec.getName())) {
                    port.setPort(portSpec.getPort());
                    port.setProtocol(portSpec.getProtocol());
                    port.setTargetPort(new IntOrString(portSpec.getTargetPort()));
                }
            });
        });
        return service;
    }

}
