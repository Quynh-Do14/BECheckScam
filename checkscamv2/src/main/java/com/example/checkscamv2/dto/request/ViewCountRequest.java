package com.example.checkscamv2.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

/**
 * Request DTO for incrementing view count of scam subjects
 * 
 * @author CheckScam Team
 * @since 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewCountRequest {
    
    /**
     * Subject identifier (phone number, bank account, or URL)
     * Examples:
     * - Phone: "0389302976"
     * - Bank: "19034567890123" 
     * - URL: "fake-bank-website.com"
     */
    @NotBlank(message = "Subject info cannot be empty")
    @Size(min = 1, max = 500, message = "Subject info must be between 1 and 500 characters")
    private String info;
    
    /**
     * Subject type identifier
     * 1 = Phone number
     * 2 = Bank account  
     * 3 = URL/Website
     */
    @NotNull(message = "Type is required")
    @Min(value = 1, message = "Type must be 1 (phone), 2 (bank), or 3 (url)")
    @Max(value = 3, message = "Type must be 1 (phone), 2 (bank), or 3 (url)")
    private Integer type;
    
    /**
     * Get human-readable type description
     * 
     * @return Type description string
     */
    public String getTypeDescription() {
        return switch (type) {
            case 1 -> "Phone Number";
            case 2 -> "Bank Account";
            case 3 -> "URL/Website";
            default -> "Unknown";
        };
    }
    
    /**
     * Validate that info format matches the type
     * 
     * @return true if format is valid for the type
     */
    public boolean isValidFormat() {
        if (info == null || type == null) {
            return false;
        }
        
        return switch (type) {
            case 1 -> isValidPhoneFormat(info);
            case 2 -> isValidBankAccountFormat(info);
            case 3 -> isValidUrlFormat(info);
            default -> false;
        };
    }
    
    private boolean isValidPhoneFormat(String phone) {
        // Vietnamese phone format: starts with 0, 10-11 digits
        return phone.matches("^0\\d{8,10}$");
    }
    
    private boolean isValidBankAccountFormat(String account) {
        // Bank account: 8-20 digits
        return account.matches("^\\d{8,20}$");
    }
    
    private boolean isValidUrlFormat(String url) {
        // Basic URL validation
        return url.matches("^[a-zA-Z0-9][a-zA-Z0-9\\-\\.]*[a-zA-Z0-9]\\.[a-zA-Z]{2,}.*$") ||
               url.matches("^https?://.*$");
    }
    
    @Override
    public String toString() {
        return String.format("ViewCountRequest{info='%s', type=%d (%s)}", 
                           info, type, getTypeDescription());
    }
}
