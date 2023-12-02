package com.mibottle.ideoperators.model;

public enum IdeRoleType {
    ADMIN("administrator"),
    ARCHITECT("architect"),
    DEVELOPER("developer"),
    CODER("coder");

    private String value;

    IdeRoleType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
