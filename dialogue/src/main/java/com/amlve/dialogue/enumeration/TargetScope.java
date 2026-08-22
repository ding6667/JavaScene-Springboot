package com.amlve.dialogue.enumeration;

import lombok.Getter;

@Getter
public enum TargetScope {
    INDIVIDUAL("individual"),
    GROUP("group"),
    ALL("all");

    private final String code;

    TargetScope(String code) {
        this.code = code;
    }

    public static TargetScope fromCode(String code) {
        for (TargetScope targetScope : values()) {
            if (targetScope.getCode().equalsIgnoreCase(code)) {
                return targetScope;
            }
        }
        return INDIVIDUAL;
    }
}
