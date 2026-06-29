package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Gender implements BaseEnum {

    MALE("MALE", "男"),
    FEMALE("FEMALE", "女"),
    UNKNOWN("UNKNOWN", "未知");

    private final String code;
    private final String desc;

    /**
     * Parse gender from Chinese label, enum code, or enum name. Returns UNKNOWN on null/unrecognized input.
     */
    public static Gender fromLabel(String label) {
        if (label == null) return UNKNOWN;
        return switch (label.trim()) {
            case "男", "MALE" -> MALE;
            case "女", "FEMALE" -> FEMALE;
            case "未知", "UNKNOWN" -> UNKNOWN;
            default -> {
                try {
                    yield Gender.valueOf(label);
                } catch (IllegalArgumentException e) {
                    yield UNKNOWN;
                }
            }
        };
    }
}
