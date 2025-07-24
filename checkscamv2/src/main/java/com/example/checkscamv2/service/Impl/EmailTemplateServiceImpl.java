package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.constant.ServiceRegistrationConstants;
import com.example.checkscamv2.dto.request.ServiceRegistrationRequest;
import com.example.checkscamv2.service.EmailTemplateService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implementation of EmailTemplateService
 * Handles email template building for service registration
 * 
 * @author CheckScam Team
 */
@Service
public class EmailTemplateServiceImpl implements EmailTemplateService {

    @Override
    public String buildAdminNotificationEmail(ServiceRegistrationRequest request) {
        StringBuilder content = new StringBuilder();
        
        // Generate unique request ID
        String requestId = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern(ServiceRegistrationConstants.REQUEST_ID_DATE_FORMAT)
        );
        
        content.append("Kính gửi Quý bộ phận,\n\n");
        content.append("Chúng tôi xin thông báo có một khách hàng đã đăng ký dịch vụ với thông tin chi tiết như sau:\n\n");
        
        // Customer information section
        appendCustomerInfo(content, request);
        
        // Service details section  
        appendServiceDetails(content, request, requestId);
        
        // Footer section
        appendFooter(content);
        
        return content.toString();
    }

    @Override
    public String buildCustomerConfirmationEmail(ServiceRegistrationRequest request) {
        StringBuilder content = new StringBuilder();
        
        content.append("Kính chào ").append(request.getName()).append(",\n\n");
        content.append("Chúng tôi xin cảm ơn bạn đã đăng ký dịch vụ của chúng tôi!\n\n");
        
        // Success header
        content.append("🎉 XÁC NHẬN ĐĂNG KÝ THÀNH CÔNG\n");
        content.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        content.append("Yêu cầu đăng ký dịch vụ của bạn đã được tiếp nhận và xử lý.\n\n");
        
        // Registration details
        appendRegistrationDetails(content, request);
        
        // Next steps
        appendNextSteps(content);
        
        // Contact information
        appendContactInfo(content);
        
        // Closing
        content.append("Nếu có thắc mắc hoặc cần hỗ trợ gấp, vui lòng liên hệ với chúng tôi qua các kênh trên.\n\n");
        content.append("Trân trọng cảm ơn và rất mong được phục vụ bạn!\n\n");
        content.append("Trân trọng,\n");
        content.append("Đội ngũ Techbyte/idai.vn");
        
        return content.toString();
    }

    private void appendCustomerInfo(StringBuilder content, ServiceRegistrationRequest request) {
        content.append("Thông tin khách hàng:\n\n");
        content.append("• Họ và tên: ").append(request.getName()).append("\n");
        content.append("• Email: ").append(request.getEmail()).append("\n");
        content.append("• Số điện thoại: ").append(request.getPhoneNumber()).append("\n");
        
        if (isNotEmpty(request.getAddress())) {
            content.append("• Địa chỉ: ").append(request.getAddress()).append("\n");
        }
        content.append("\n");
    }

    private void appendServiceDetails(StringBuilder content, ServiceRegistrationRequest request, String requestId) {
        content.append("Chi tiết yêu cầu:\n\n");
        
        if (isNotEmpty(request.getServiceDescription())) {
            content.append("• Mô tả: ").append(request.getServiceDescription()).append("\n");
        }
        
        if (isNotEmpty(request.getServicePackage())) {
            content.append("• Gói dịch vụ: ").append(request.getServicePackage()).append("\n");
        }
        
        content.append("• Giá: 0 VND\n");
        content.append("• Thời gian đăng ký: ").append(requestId).append("\n\n");
        
        content.append("Vui lòng liên hệ khách hàng để xác nhận thông tin và tiến hành xử lý yêu cầu trong thời gian sớm nhất.\n\n");
    }

    private void appendFooter(StringBuilder content) {
        content.append("Trân trọng,\n\n");
        content.append(ServiceRegistrationConstants.COMPANY_NAME).append("\n\n");
        content.append("Email: ").append(ServiceRegistrationConstants.COMPANY_EMAIL);
        content.append(" | Hotline: ").append(ServiceRegistrationConstants.COMPANY_HOTLINE);
    }

    private void appendRegistrationDetails(StringBuilder content, ServiceRegistrationRequest request) {
        content.append("📝 THÔNG TIN ĐĂNG KÝ\n");
        content.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        content.append("• Họ và tên: ").append(request.getName()).append("\n");
        content.append("• Email: ").append(request.getEmail()).append("\n");
        content.append("• Số điện thoại: ").append(request.getPhoneNumber()).append("\n");
        
        if (isNotEmpty(request.getAddress())) {
            content.append("• Địa chỉ: ").append(request.getAddress()).append("\n");
        }
        
        if (isNotEmpty(request.getServiceDescription())) {
            content.append("• Dịch vụ quan tâm: ").append(request.getServiceDescription()).append("\n");
        }
        
        if (isNotEmpty(request.getServicePackage())) {
            content.append("• Gói dịch vụ: ").append(request.getServicePackage()).append("\n");
        }
        content.append("\n");
    }

    private void appendNextSteps(StringBuilder content) {
        content.append("🕒 CÁC BƯỚC TIẾP THEO\n");
        content.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        content.append("• Chúng tôi sẽ liên hệ lại với bạn trong vòng 24 giờ\n");
        content.append("• Đội ngũ chuyên viên sẽ tư vấn chi tiết về dịch vụ\n");
        content.append("• Bạn sẽ nhận được báo giá phù hợp với nhu cầu\n\n");
    }

    private void appendContactInfo(StringBuilder content) {
        content.append("📞 THÔNG TIN LIÊN HỆ\n");
        content.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        content.append("• Email: ").append(ServiceRegistrationConstants.COMPANY_EMAIL).append("\n");
        content.append("• Hotline: ").append(ServiceRegistrationConstants.COMPANY_HOTLINE).append("\n");
        content.append("• Website: ").append(ServiceRegistrationConstants.COMPANY_WEBSITE).append("\n\n");
    }

    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
