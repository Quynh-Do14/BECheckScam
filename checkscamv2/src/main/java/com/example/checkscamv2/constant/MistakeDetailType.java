package com.example.checkscamv2.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MistakeDetailType {
    PHONE_NUMBER(1),
    BANK_ACCOUNT(2),
    URL(3);

    private final int value;

    MistakeDetailType(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static MistakeDetailType fromValue(int value) {
        for (MistakeDetailType type : MistakeDetailType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid MistakeDetailType value: " + value);
    }
}