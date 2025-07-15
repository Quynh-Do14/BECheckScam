package com.example.checkscamv2.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregated response DTO containing all types of top scams
 * Used for the /top-all endpoint to provide comprehensive scam data
 * 
 * @author CheckScam Team
 * @since 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopScamsAggregateResponseDTO {
    
    /**
     * Top phone number scams ranked by view count
     */
    private List<TopScamItemResponseDTO> phones;
    
    /**
     * Top bank account scams ranked by view count
     */
    private List<TopScamItemResponseDTO> banks;
    
    /**
     * Top URL/website scams ranked by view count
     */
    private List<TopScamItemResponseDTO> urls;
    
    /**
     * Total count of all scam items across all types
     */
    private Integer totalCount;
    
    /**
     * Timestamp when this aggregate was generated
     */
    @Builder.Default
    private LocalDateTime generatedAt = LocalDateTime.now();
    
    /**
     * Summary statistics for the aggregate response
     */
    private ScamSummaryStats summary;
    
    /**
     * Get count of phone scams
     */
    public int getPhoneCount() {
        return phones != null ? phones.size() : 0;
    }
    
    /**
     * Get count of bank scams
     */
    public int getBankCount() {
        return banks != null ? banks.size() : 0;
    }
    
    /**
     * Get count of URL scams
     */
    public int getUrlCount() {
        return urls != null ? urls.size() : 0;
    }
    
    /**
     * Calculate and set summary statistics
     */
    public void calculateSummary() {
        this.summary = ScamSummaryStats.builder()
                .phoneCount(getPhoneCount())
                .bankCount(getBankCount())
                .urlCount(getUrlCount())
                .totalCount(getPhoneCount() + getBankCount() + getUrlCount())
                .mostActiveCategory(determineMostActiveCategory())
                .build();
    }
    
    private String determineMostActiveCategory() {
        int phoneCount = getPhoneCount();
        int bankCount = getBankCount();
        int urlCount = getUrlCount();
        
        if (phoneCount >= bankCount && phoneCount >= urlCount) {
            return "phones";
        } else if (bankCount >= urlCount) {
            return "banks";
        } else {
            return "urls";
        }
    }
    
    /**
     * Inner class for summary statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScamSummaryStats {
        private Integer phoneCount;
        private Integer bankCount;
        private Integer urlCount;
        private Integer totalCount;
        private String mostActiveCategory;
    }
}
