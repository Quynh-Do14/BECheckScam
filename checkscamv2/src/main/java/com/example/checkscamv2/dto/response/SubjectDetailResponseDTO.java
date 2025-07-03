package com.example.checkscamv2.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectDetailResponseDTO {
    private String info;
    private Integer type; // 1: phone, 2: bank, 3: url
    private String name;
    private String description;
    private Long totalScamAmount;
    private Integer reportCount;
    private LocalDateTime lastReportDate;
    private String riskLevel; // "low", "medium", "high"
    private List<String> evidenceImages;
    private List<ReportItemDTO> reports;
    private List<RelatedSubjectDTO> relatedSubjects;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReportItemDTO {
        private Long id;
        private LocalDateTime date;
        private String description;
        private Long amount;
        private String reporterLocation;
        private String status;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelatedSubjectDTO {
        private String info;
        private Integer type;
        private String description;
        private String riskLevel;
    }
    
    private String determineRiskLevel(Integer reportCount) {
        if (reportCount == null || reportCount == 0) return "low";
        if (reportCount >= 10) return "high";
        if (reportCount >= 3) return "medium";
        return "low";
    }
}