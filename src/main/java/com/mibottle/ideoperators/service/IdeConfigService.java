package com.mibottle.ideoperators.service;

import com.mibottle.ideoperators.customresource.IdeConfig;
import com.mibottle.ideoperators.customresource.IdeConfigSpec;
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;


@Service
@Slf4j
public class IdeConfigService {

    private final KubernetesClient client;

    public static final class IdeConfigList extends DefaultKubernetesResourceList<IdeConfig> {
    }
    public IdeConfigService(KubernetesClient client) {
        this.client = client;
    }

    public IdeConfig createIdeConfig(String namespace, String ideConfigName, IdeConfigSpec ideConfigSpec) {
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
}
