package com.example.checkscamv2.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporterRankingResponseDTO {
    private Long id;
    private String email;
    private Integer totalReports;
    private Integer approvedReports;
    private Double successRate;
    private Integer rank;
    private String lastReportDate;
}
