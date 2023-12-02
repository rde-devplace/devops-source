package com.mibottle.ideoperators.customresource;

import io.fabric8.kubernetes.api.model.IntOrString;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Port {
    private String name;
    private String protocol;
    private int port;
    private int targetPort;
}
