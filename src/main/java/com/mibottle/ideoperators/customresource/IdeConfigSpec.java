package com.mibottle.ideoperators.customresource;

import lombok.Data;

import java.util.List;

@Data
public class IdeConfigSpec {

    private String userName;
    private List<String> serviceTypes;
    private WebSSH webssh;
    private Vscode vscode;
    private List<Port> portList;
    private InfrastructureSize infrastructureSize;
    private int replicas;
}

