package com.example.checkscamv2.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExternalUrlCheckResponse {
    private String provider;
    private boolean isMalicious;
}