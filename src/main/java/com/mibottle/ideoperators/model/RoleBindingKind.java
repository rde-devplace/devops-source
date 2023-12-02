package com.mibottle.ideoperators.model;

public enum RoleBindingKind {
    NAMESPACE("RoleBinding"),
    CLUSTER("ClusterRoleBinding");

    private final String value;

    RoleBindingKind(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
