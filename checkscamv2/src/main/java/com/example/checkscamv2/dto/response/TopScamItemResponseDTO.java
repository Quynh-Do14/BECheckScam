package com.example.checkscamv2.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopScamItemResponseDTO {
    private Long id;
    private String info; // phone number, bank account, hoặc URL
    private String description; // mô tả ngắn gọn
    private Integer verifiedCount; // số lượt báo cáo
    private Integer viewCount; // số lượt xem
    private LocalDateTime lastReportAt; // lần báo cáo gần nhất
    private String status; // "danger", "warning", "safe"
    private String type; // "phone", "bank", "url"
    
    // Constructor cho Phone Scam
    public TopScamItemResponseDTO(Long id, String phoneNumber, String ownerName, 
                                 Integer verifiedCount, Integer viewCount, 
                                 LocalDateTime lastReportAt) {
        this.id = id;
        this.info = phoneNumber;
        this.description = ownerName != null ? ownerName : "Số điện thoại nghi vấn";
        this.verifiedCount = verifiedCount;
        this.viewCount = viewCount;
        this.lastReportAt = lastReportAt;
        this.type = "phone";
        this.status = determineStatus(verifiedCount);
    }
    
    // Constructor cho Bank Scam
    public TopScamItemResponseDTO(Long id, String bankAccount, String bankName, 
                                 String nameAccount, Integer verifiedCount, 
                                 Integer viewCount, LocalDateTime lastReportAt) {
        this.id = id;
        this.info = bankAccount;
        this.description = (bankName != null ? bankName + " - " : "") + 
                          (nameAccount != null ? nameAccount : "Tài khoản nghi vấn");
        this.verifiedCount = verifiedCount;
        this.viewCount = viewCount;
        this.lastReportAt = lastReportAt;
        this.type = "bank";
        this.status = determineStatus(verifiedCount);
    }
    
    // Constructor cho URL Scam
    public TopScamItemResponseDTO(Long id, String url, Integer verifiedCount, 
                                 Integer viewCount, LocalDateTime lastReportAt) {
        this.id = id;
        this.info = url;
        this.description = "Website nghi vấn";
        this.verifiedCount = verifiedCount;
        this.viewCount = viewCount;
        this.lastReportAt = lastReportAt;
        this.type = "url";
        this.status = determineStatus(verifiedCount);
    }
    
    private String determineStatus(Integer count) {
        if (count == null || count == 0) return "safe";
        if (count >= 10) return "danger";
        if (count >= 3) return "warning";
        return "safe";
    }
}
