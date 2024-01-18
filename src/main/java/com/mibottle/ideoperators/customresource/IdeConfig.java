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

