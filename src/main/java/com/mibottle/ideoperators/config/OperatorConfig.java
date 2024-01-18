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
package com.mibottle.ideoperators.config;

import com.mibottle.ideoperators.reconciler.IdeResourceGenerator;
import com.mibottle.ideoperators.reconciler.VscodeConfigReconciler;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OperatorConfig {



    @Bean
    public VscodeConfigReconciler vscodeConfigReconciler(KubernetesClient client) {
        return new VscodeConfigReconciler(client, new IdeResourceGenerator());
    }


    @Bean(initMethod = "start", destroyMethod = "stop")
    public Operator operator(List<Reconciler> controllers) {
        Operator operator = new Operator();
        controllers.forEach(operator::register);
        return operator;
    }

    @Bean
    public KubernetesClient kubernetesClient() {
        return new DefaultKubernetesClient();
    }
}
