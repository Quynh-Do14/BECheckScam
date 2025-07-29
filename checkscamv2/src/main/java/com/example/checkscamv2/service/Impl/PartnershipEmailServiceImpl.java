package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.constant.ServiceRegistrationConstants;
import com.example.checkscamv2.dto.request.PartnershipRequest;
import com.example.checkscamv2.enums.PackageType;
import com.example.checkscamv2.service.EmailService;
import com.example.checkscamv2.service.PartnershipEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnershipEmailServiceImpl implements PartnershipEmailService {
    
    private final EmailService emailService;
    
    @Override
    public boolean sendPartnershipNotificationToAdmin(PartnershipRequest request) {
        try {
            String subject = "[AI6] Yêu cầu hợp tác mới từ " + request.getOrganization();
            String content = buildAdminNotificationContent(request);
            
            // Gửi email cho tất cả admin
            boolean success = true;
            for (String adminEmail : ServiceRegistrationConstants.ADMIN_EMAILS) {
                try {
                    emailService.sendEmail(adminEmail, subject, content);
                    log.info("Sent partnership notification to admin {} for organization: {}", adminEmail, request.getOrganization());
                } catch (Exception e) {
                    log.error("Failed to send partnership notification to admin {}: {}", adminEmail, e.getMessage());
                    success = false;
                }
            }
            return success;
        } catch (Exception e) {
            log.error("Failed to send partnership notification to admins: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean sendPartnershipConfirmationToClient(PartnershipRequest request) {
        try {
            String subject = "Xác nhận đăng ký hợp tác với AI6 - " + request.getOrganization();
            String content = buildClientConfirmationContent(request);
            
            emailService.sendEmail(request.getEmail(), subject, content);
            log.info("Sent partnership confirmation to client: {}", request.getEmail());
            return true;
        } catch (Exception e) {
            log.error("Failed to send partnership confirmation to client: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    @Async
    public void sendPartnershipEmails(PartnershipRequest request) {
        log.info("Sending partnership emails asynchronously for: {}", request.getOrganization());
        
        // Gửi email cho admin
        try {
            sendPartnershipNotificationToAdmin(request);
        } catch (Exception e) {
            log.error("Error sending admin notification: {}", e.getMessage());
        }
        
        // Gửi email xác nhận cho client
        try {
            sendPartnershipConfirmationToClient(request);
        } catch (Exception e) {
            log.error("Error sending client confirmation: {}", e.getMessage());
        }
        
        log.info("Partnership emails sent successfully for: {}", request.getOrganization());
    }
    
    private String buildAdminNotificationContent(PartnershipRequest request) {
        PackageType packageType;
        try {
            packageType = PackageType.fromCode(request.getPackageType());
        } catch (Exception e) {
            packageType = PackageType.BASIC; // fallback
        }
        
        return String.format(
                "Kính gửi Admin,\n\n" +
                "Có yêu cầu hợp tác mới từ khách hàng:\n\n" +
                "📋 THÔNG TIN KHÁCH HÀNG:\n" +
                "• Họ tên: %s\n" +
                "• Email: %s\n" +
                "• Tổ chức: %s\n" +
                "• Số điện thoại: %s\n" +
                "• Gói hợp tác: %s (%s)\n" +
                "• Thời gian đăng ký: %s\n\n" +
                "💬 LỜI NHẮN:\n" +
                "%s\n\n" +
                "---\n" +
                "Vui lòng liên hệ trực tiếp với khách hàng để thảo luận chi tiết về hợp tác.\n" +
                "📞 Hotline: %s\n" +
                "📧 Email: %s\n\n" +
                "Trân trọng,\n" +
                "Hệ thống AI6 Partnership",
                request.getName(),
                request.getEmail(),
                request.getOrganization(),
                request.getPhoneNumber() != null ? request.getPhoneNumber() : "Chưa cung cấp",
                packageType.getDisplayName(),
                packageType.getMinAmount().toString() + " VND",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern(ServiceRegistrationConstants.DISPLAY_DATE_FORMAT)),
                request.getMessage() != null ? request.getMessage() : "Không có lời nhắn",
                ServiceRegistrationConstants.COMPANY_HOTLINE,
                ServiceRegistrationConstants.COMPANY_EMAIL
        );
    }
    
    private String buildClientConfirmationContent(PartnershipRequest request) {
        PackageType packageType;
        try {
            packageType = PackageType.fromCode(request.getPackageType());
        } catch (Exception e) {
            packageType = PackageType.BASIC; // fallback
        }
        
        return String.format(
                "Kính gửi %s,\n\n" +
                "Cảm ơn bạn đã quan tâm và đăng ký hợp tác với AI6!\n\n" +
                "📋 THÔNG TIN ĐĂNG KÝ:\n" +
                "• Tổ chức: %s\n" +
                "• Gói hợp tác: %s\n" +
                "• Email liên hệ: %s\n" +
                "• Thời gian đăng ký: %s\n\n" +
                "✅ TRẠNG THÁI: Đã tiếp nhận yêu cầu\n\n" +
                "🚀 BƯỚC TIẾP THEO:\n" +
                "Đội ngũ AI6 sẽ xem xét đề xuất của bạn và liên hệ trực tiếp trong vòng 24-48 giờ làm việc " +
                "để thảo luận chi tiết về:\n" +
                "• Điều khoản hợp tác\n" +
                "• Quyền lợi cụ thể\n" +
                "• Kế hoạch triển khai\n" +
                "• Thỏa thuận pháp lý\n\n" +
                "📞 LIÊN HỆ KHẨN CẤP:\n" +
                "• Email: %s\n" +
                "• Hotline: %s\n" +
                "• Website: %s\n\n" +
                "Chúng tôi rất mong được hợp tác cùng %s trong việc xây dựng một Việt Nam an toàn hơn trước " +
                "các mối đe dọa lừa đảo trực tuyến! 🤖⚔️\n\n" +
                "Trân trọng,\n" +
                "%s\n" +
                "AI6 Partnership Team",
                
                request.getName(),
                request.getOrganization(),
                packageType.getDisplayName(),
                request.getEmail(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern(ServiceRegistrationConstants.DISPLAY_DATE_FORMAT)),
                ServiceRegistrationConstants.COMPANY_EMAIL,
                ServiceRegistrationConstants.COMPANY_HOTLINE,
                ServiceRegistrationConstants.COMPANY_WEBSITE,
                request.getOrganization(),
                ServiceRegistrationConstants.COMPANY_NAME
        );
    }
}