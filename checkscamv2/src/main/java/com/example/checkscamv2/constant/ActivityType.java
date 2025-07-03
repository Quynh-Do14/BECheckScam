package com.example.checkscamv2.constant;

public enum ActivityType {
    UPLOAD("upload"),  // Thay POST thành UPLOAD để khớp với database
    REPORT("report"), 
    JOIN("join");
    
    private final String value;
    
    ActivityType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static ActivityType fromString(String value) {
        for (ActivityType type : ActivityType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown activity type: " + value);
    }
}