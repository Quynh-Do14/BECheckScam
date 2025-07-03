package com.example.checkscamv2.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScamAnalysisResponse {
    private String info;
    private Integer type;
    private String description;
    private String reportDescription;
    private String moneyScam;
    private LocalDateTime dateReport;
    private Integer verifiedCount; // Số lần xác thực
    private LocalDateTime lastReportAt;
    private List<String> evidenceUrls;
    private String analysis;
    private String screenShot;
    private List<ExternalUrlCheckResponse> externalUrlCheckResponses;
} 
