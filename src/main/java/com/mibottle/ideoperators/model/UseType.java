package com.mibottle.ideoperators.model;

// Permission configuration
public enum UseType {
    CREATE("create"),
    USE("use");

    private String value;

    UseType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
