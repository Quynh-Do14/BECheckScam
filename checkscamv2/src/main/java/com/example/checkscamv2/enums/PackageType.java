package com.example.checkscamv2.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PackageType {
    BASIC("basic", "Hợp Tác Cơ Bản", 10000000L),
    COMPANION("companion", "Hợp Tác Đồng Hành", 10000000L),
    COLLABORATION("collaboration", "Hợp Tác Thân Thiệt", 20000000L),
    STRATEGIC("strategic", "Hợp Tác Chiến Lược", 40000000L),
    GOLD("gold", "Hợp Tác Vàng", 80000000L);
    
    private final String code;
    private final String displayName;
    private final Long minAmount;
    
    public static PackageType fromCode(String code) {
        for (PackageType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid package type: " + code);
    }
}