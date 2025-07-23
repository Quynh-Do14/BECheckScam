package com.example.checkscamv2.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {
    private String transactionId;
    private String message;
    private LocalDateTime createdAt;
    private boolean emailSent;


}
