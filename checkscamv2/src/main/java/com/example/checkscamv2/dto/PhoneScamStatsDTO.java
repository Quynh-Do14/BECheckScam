package com.example.checkscamv2.dto;


import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PhoneScamStatsDTO {
    private Long phoneScamId;
    private Integer verifiedCount;
    private LocalDateTime lastReportAt;
}
