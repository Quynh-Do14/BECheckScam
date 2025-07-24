package com.example.checkscamv2.controller;

import com.example.checkscamv2.constant.ServiceRegistrationConstants;
import com.example.checkscamv2.dto.request.ServiceRegistrationRequest;
import com.example.checkscamv2.dto.ResponseHelper;
import com.example.checkscamv2.dto.response.ResponseObject;
import com.example.checkscamv2.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/service")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ServiceRegistrationController {

    private final EmailService emailService;
    private final ResponseHelper responseHelper;

    /**
     * API đăng ký dịch vụ từ khách hàng
     * POST /api/service/register
     */
    @PostMapping("/register")
    public ResponseEntity<ResponseObject> registerService(@Valid @RequestBody ServiceRegistrationRequest request) {
        try {
            log.info("Nhận yêu cầu đăng ký dịch vụ từ: {} - Email: {}", 
                    request.getName(), request.getEmail());

            // Gửi email thông báo đến admin
            boolean adminNotified = emailService.sendServiceRegistrationNotification(request);
            
            // Gửi email xác nhận đến khách hàng
            boolean customerConfirmed = emailService.sendServiceRegistrationConfirmation(request);

            if (adminNotified && customerConfirmed) {
                log.info("✅ Gửi email thành công cho cả admin và khách hàng: {}", request.getEmail());
                
                return responseHelper.createSuccessResponse(
                    null,
                    ServiceRegistrationConstants.SUCCESS_MESSAGE
                );
            } else {
                log.error("❌ Gửi email thất bại - Admin: {}, Customer: {}", adminNotified, customerConfirmed);
                
                return responseHelper.createErrorResponse(
                    ServiceRegistrationConstants.PARTIAL_SUCCESS_MESSAGE
                );
            }

        } catch (Exception e) {
            log.error("❌ Lỗi khi xử lý yêu cầu đăng ký dịch vụ: {}", e.getMessage(), e);
            
            return responseHelper.createErrorResponse(
                ServiceRegistrationConstants.ERROR_MESSAGE
            );
        }
    }

    /**
     * API kiểm tra trạng thái dịch vụ
     * GET /api/service/status
     */
    @GetMapping("/status")
    public ResponseEntity<ResponseObject> getServiceStatus() {
        try {
            return responseHelper.createSuccessResponse(
                ServiceRegistrationConstants.SERVICE_STATUS_ACTIVE,
                ServiceRegistrationConstants.SERVICE_ACTIVE_MESSAGE
            );
        } catch (Exception e) {
            return responseHelper.createErrorResponse(
                "Lỗi khi kiểm tra trạng thái dịch vụ"
            );
        }
    }

}
