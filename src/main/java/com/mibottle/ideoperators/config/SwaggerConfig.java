package com.mibottle.ideoperators.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI publicApi() {
        return new OpenAPI()
                .addServersItem(new Server().url("https://kube-proxy.amdp-dev.skamdp.org").description("remote Server"));
    }
}
