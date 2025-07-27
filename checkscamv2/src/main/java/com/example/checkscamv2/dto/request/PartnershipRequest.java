package com.example.checkscamv2.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnershipRequest {
    
    @NotBlank(message = "Họ tên không được để trống")
    private String name;
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
    
    @NotBlank(message = "Tổ chức không được để trống")
    private String organization;
    
    @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;
    
    @NotBlank(message = "Gói hợp tác không được để trống")
    @Pattern(regexp = "^(basic|companion|collaboration|strategic|gold)$", 
             message = "Gói hợp tác không hợp lệ")
    private String packageType;
    
    private String message;
}