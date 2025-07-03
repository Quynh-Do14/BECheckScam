package com.example.checkscamv2.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private int status;
    private long timestamp;
    // Có thể thêm code lỗi, path, v.v.
}