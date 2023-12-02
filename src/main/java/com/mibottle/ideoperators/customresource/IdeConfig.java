package com.mibottle.ideoperators.customresource;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1")
@Group("amdev.cloriver.io")
public class IdeConfig extends CustomResource<IdeConfigSpec, IdeConfigStatus> implements Namespaced {

    public IdeConfig() {
        super();
    }
    public IdeConfig(String name, IdeConfigSpec spec) {
        setMetadata(new ObjectMetaBuilder().withName(name).build());
        setSpec(spec);
    }

    @Override
    public String toString() {
        return "IdeConfig{" +
                "apiVersion='" + getApiVersion() + '\'' +
                ", kind='" + getKind() + '\'' +
                ", metadata=" + getMetadata() +
                ", spec=" + getSpec() +
                ", status=" + getStatus() +
                '}';
    }

    @Override
    protected IdeConfigStatus initStatus() {
        return new IdeConfigStatus();
    }


}

