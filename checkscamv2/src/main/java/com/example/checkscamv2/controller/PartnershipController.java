package com.example.checkscamv2.controller;

import com.example.checkscamv2.dto.request.PartnershipRequest;
import com.example.checkscamv2.dto.response.ResponseObject;
import com.example.checkscamv2.service.PartnershipEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/partnerships")
@RequiredArgsConstructor
@Slf4j
public class PartnershipController {
    
    private final PartnershipEmailService partnershipEmailService;
    
    @PostMapping("/register")
    public ResponseEntity<ResponseObject> registerPartnership(@Valid @RequestBody PartnershipRequest request) {
        log.info("Received partnership registration request from: {}", request.getOrganization());
        
        try {
            // Gửi email bất đồng bộ cho cả client và admin
            partnershipEmailService.sendPartnershipEmails(request);
            
            // Tạo response data với thông tin đăng ký
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("registrationTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            responseData.put("organization", request.getOrganization());
            responseData.put("packageType", request.getPackageType());
            responseData.put("email", request.getEmail());
            responseData.put("status", "SUBMITTED");
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.CREATED)
                            .message("Đăng ký hợp tác thành công! Chúng tôi đã gửi email xác nhận và sẽ liên hệ với bạn trong 24-48 giờ.")
                            .data(responseData)
                            .build());
        } catch (Exception e) {
            log.error("Error processing partnership registration: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Có lỗi xảy ra trong quá trình xử lý. Vui lòng thử lại sau hoặc liên hệ trực tiếp qua email.")
                            .build());
        }
    }
}