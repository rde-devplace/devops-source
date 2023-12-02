package com.mibottle.ideoperators.customresource;

import lombok.Data;

import java.util.Map;

@Data
public class Permission {
    private String useType;
    private String scope;
    private String role;
    private String serviceAccountName;
    private Map<String, Object> additionalProperties;
}
