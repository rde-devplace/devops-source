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
package com.mibottle.ideoperators.service;

import com.mibottle.ideoperators.customresource.IdeConfig;
import com.mibottle.ideoperators.customresource.IdeConfigSpec;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
@Slf4j
public class IdeConfigService {

    @Value("${ide.ide-proxy-domain}")
    private String ideProxyDomain;

    private final KubernetesClient client;

    public static final class IdeConfigList extends DefaultKubernetesResourceList<IdeConfig> {
    }
    public IdeConfigService(KubernetesClient client) {
        this.client = client;
    }

    public IdeConfig createIdeConfig(String namespace, String ideConfigName, String packageType, String proxyDomain, IdeConfigSpec ideConfigSpec) {
        log.info("Creating IdeConfig: " + ideConfigSpec.toString());
        NonNamespaceOperation<IdeConfig, IdeConfigList, Resource<IdeConfig>> ideConfigs =
                client.resources(IdeConfig.class, IdeConfigList.class).inNamespace(namespace);

        // Check if any field of ideConfigSpec is null
        boolean hasNullField = Arrays.stream(ideConfigSpec.getClass().getDeclaredFields())
                .anyMatch(field -> {
                    field.setAccessible(true); // in case the field is private
                    try {
                        return field.get(ideConfigSpec) == null;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        return false;
                    }
                });

        IdeConfig ideConfig = new IdeConfig(ideConfigName, ideConfigSpec);
        if(proxyDomain.equals("env")) proxyDomain = ideProxyDomain;
        ideConfig.getMetadata().setAnnotations(Map.of("packageType.cloriver.io/vscode", packageType, "proxyDomain.cloriver.io/vscode", proxyDomain));
        if (!hasNullField) {
            ideConfigs.resource(ideConfig).create();
        } else {
            log.warn("IdeConfigSpec has a null field. Skipping ideConfig creation.");
        }

        ideConfigs.list().getItems()
                .forEach(s -> System.out.printf(" - %s%n", s.getSpec().getUserName()));

        return ideConfig;
    }

    public IdeConfig deleteIdeConfig(String namespace, String ideConfigName) {
        NonNamespaceOperation<IdeConfig, IdeConfigList, Resource<IdeConfig>> ideConfigs =
                client.resources(IdeConfig.class, IdeConfigList.class).inNamespace(namespace);

        IdeConfig ideConfig = new IdeConfig(ideConfigName, null);
        ideConfigs.resource(ideConfig).delete();
        ideConfigs.list().getItems()
                .forEach(s -> System.out.printf(" - %s%n", s.getSpec().getUserName()));

        return ideConfig;
    }

    public String deletePVC(String namespace, String pvcName) {
        List<StatusDetails> isDeleted = client.persistentVolumeClaims().inNamespace(namespace).withName(pvcName).delete();

        if (isDeleted.isEmpty()) {
            log.info("PVC not found: " + pvcName + " in namespace: " + namespace);
            return null;
        } else {
            log.info("PVC deleted successfully: " + pvcName);
        }

        return pvcName;
    }

    private List<IdeConfig> fetchIdeConfigs(String namespace) {
        NonNamespaceOperation<IdeConfig, IdeConfigList, Resource<IdeConfig>> ideConfigs =
                client.resources(IdeConfig.class, IdeConfigList.class).inNamespace(namespace);

        return ideConfigs.list().getItems();
    }

    public List<IdeConfig> getIdeConfigs(String namespace) {
        List<IdeConfig> configs = fetchIdeConfigs(namespace);

        List<IdeConfig> configList = new ArrayList<>();
        if (configs.isEmpty()) {
            log.info("No IdeConfigs found in namespace: " + namespace);
        } else {
            for (IdeConfig config : configs) {
                if (config != null) {
                    configList.add(config);
                    log.info("Found IdeConfig: " + config.getMetadata());
                }
            }
        }

        return configList;
    }

    // 조건에 따라 검색하게
    // null이 아닌 것을 찾아서 ...
    public List<IdeConfig> getIdeConfigs(String namespace, String userName, String wsName, String appName) {
        List<IdeConfig> configs = fetchIdeConfigs(namespace);
        List<IdeConfig> matchingConfig = new ArrayList<>();

        if (configs.isEmpty()) {
            log.info("No IdeConfigs found in namespace: " + namespace);
        } else {
            for (IdeConfig config : configs) {
                if (config != null &&
                        (userName == null || userName.equals(config.getSpec().getUserName())) &&
                        (wsName == null || wsName.equals(config.getSpec().getWsName())) &&
                        (appName == null || appName.equals(config.getSpec().getAppName()))) {
                    matchingConfig.add(config);
                    log.info("Found matching IdeConfig: " + config.getMetadata());
                }
            }
        }

        return matchingConfig;
    }

    // 현재 상태를 가져 오는 메서드 예시
    public IdeConfig getIdeConfig(String namespace, String ideConfigName) {
        try {
            // Kubernetes 클라이언트를 사용하여 현재 상태 가져오기
            return client.resources(IdeConfig.class, IdeConfigService.IdeConfigList.class).inNamespace(namespace).withName(ideConfigName).get();
        } catch (KubernetesClientException e) {
            // 예외 처리
            log.error("Failed to get current state of resource {} in namespace {}", ideConfigName, namespace, e);
            return null;
        }
    }


    // updateIdeConfig 메소드 추가
    public Optional<IdeConfig> updateIdeConfig(String namespace, String ideConfigName, IdeConfigSpec ideConfigSpec) {
        log.info("Updating IdeConfig: " + ideConfigName + " in namespace: " + namespace);
        NonNamespaceOperation<IdeConfig, IdeConfigList, Resource<IdeConfig>> ideConfigs =
                client.resources(IdeConfig.class, IdeConfigList.class).inNamespace(namespace);

        Resource<IdeConfig> ideConfigResource = ideConfigs.withName(ideConfigName);
        IdeConfig existingIdeConfig = ideConfigResource.get();
        if (existingIdeConfig == null) {
            log.warn("IdeConfig not found: " + ideConfigName + " in namespace: " + namespace);
            return Optional.empty();
        }

        // 업데이트할 IdeConfigSpec의 null이 아닌 필드만 적용
        IdeConfigSpec configSpec = applyNonNullFields(existingIdeConfig.getSpec(), ideConfigSpec);

        // Kubernetes 클라이언트를 사용하여 업데이트
        ideConfigResource.edit(c -> {
            c.setSpec(configSpec);
            return c;
        });

        log.info("IdeConfig updated successfully: " + ideConfigName);
        return Optional.ofNullable(ideConfigResource.get());
    }

    // null이 아닌 필드만 적용하는 메소드
    private IdeConfigSpec applyNonNullFields(IdeConfigSpec existingSpec, IdeConfigSpec newSpec) {
        if (newSpec.getUserName() != null) {
            existingSpec.setUserName(newSpec.getUserName());
        }
        if (newSpec.getServiceTypes() != null && !newSpec.getServiceTypes().isEmpty()) {
            existingSpec.setServiceTypes(newSpec.getServiceTypes());
        }

        if(newSpec.getVscode() != null) {
            existingSpec.setVscode(newSpec.getVscode());
        }
        if(newSpec.getWebssh() != null) {
            existingSpec.setWebssh(newSpec.getWebssh());
        }
        if(newSpec.getPortList() != null && !newSpec.getPortList().isEmpty()) {
            existingSpec.setPortList(newSpec.getPortList());
        }
        if(newSpec.getInfrastructureSize() != null) {
            existingSpec.setInfrastructureSize(newSpec.getInfrastructureSize());
        }

        if(newSpec.getReplicas() >= 0 ) {
            existingSpec.setReplicas(newSpec.getReplicas());
        }

        return existingSpec;
    }


    public List<PodStatus> getPodStatusInIdeConfig(String namespace, String ideConfigName) {
        List<PodStatus> podStatusList = new ArrayList<>();
        String labelSelector = ideConfigName + "-rde";
        client.pods().inNamespace(namespace).withLabel("app", labelSelector).list().getItems()
                .forEach(pod -> {
                    PodStatus podStatus = pod.getStatus();
                    podStatusList.add(pod.getStatus());
                    log.info("Pod status: " + podStatus);

                });

        return podStatusList;
    }

    /*

        public List<IdeConfigSpec> getIdeConfigs(String namespace) {
        List<IdeConfig> configs = fetchIdeConfigs(namespace);

        List<IdeConfigSpec> specList = new ArrayList<>();
        if (configs.isEmpty()) {
            log.info("No IdeConfigs found in namespace: " + namespace);
        } else {
            for (IdeConfig config : configs) {
                IdeConfigSpec spec = config.getSpec();
                if (spec != null) {
                    specList.add(spec);
                    log.info("Found IdeConfig: " + config.getMetadata());
                }
            }
        }

        return specList;
    }

    public List<IdeConfigSpec> getIdeConfig(String namespace, String ideConfigName) {
        List<IdeConfig> configs = fetchIdeConfigs(namespace);

        List<IdeConfigSpec> matchingSpecs = new ArrayList<>();
        if (configs.isEmpty()) {
            log.info("No IdeConfigs found in namespace: " + namespace);
        } else {
            for (IdeConfig config : configs) {
                IdeConfigSpec spec = config.getSpec();
                if (spec != null && ideConfigName.equals(spec.getUserName())) {
                    matchingSpecs.add(spec);
                    log.info("Found matching IdeConfig: " + config.getMetadata());
                }
            }
        }

        return matchingSpecs;
    }

     */

}
