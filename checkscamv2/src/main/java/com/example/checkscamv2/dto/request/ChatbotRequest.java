package com.example.checkscamv2.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotRequest {
    private String message;
}