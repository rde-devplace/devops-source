package com.mibottle.ideoperators.model;

public enum ScopeType {
    NAMESPACE("namespace"),
    CLUSTER("cluster");

    private String value;

    ScopeType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
