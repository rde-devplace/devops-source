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
