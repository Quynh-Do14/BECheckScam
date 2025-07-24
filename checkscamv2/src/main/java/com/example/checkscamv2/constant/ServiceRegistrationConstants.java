package com.example.checkscamv2.constant;

/**
 * Constants for Service Registration functionality
 * 
 * @author CheckScam Team
 */
public final class ServiceRegistrationConstants {

    // Private constructor to prevent instantiation
    private ServiceRegistrationConstants() {
        throw new IllegalStateException("Utility class");
    }

    // Admin Emails
    public static final String[] ADMIN_EMAILS = {
        "admin@hotrodoan.vn", 
            "buithitrangyb@gmail.com"
    };

    // Email Subjects
    public static final String ADMIN_EMAIL_SUBJECT = "🔔 Thông báo đăng ký dịch vụ từ khách hàng";
    public static final String CUSTOMER_EMAIL_SUBJECT = "✅ Xác nhận đăng ký dịch vụ thành công";

    // Contact Information
    public static final String COMPANY_EMAIL = "ai@idai.vn";
    public static final String COMPANY_HOTLINE = "0973.454.140";
    public static final String COMPANY_WEBSITE = "https://idai.vn";
    public static final String COMPANY_NAME = "[Techbyte/idai.vn]";

    // Response Messages
    public static final String SUCCESS_MESSAGE = "Đăng ký dịch vụ thành công! Chúng tôi đã gửi email xác nhận và sẽ liên hệ với bạn trong thời gian sớm nhất.";
    public static final String PARTIAL_SUCCESS_MESSAGE = "Đăng ký thành công nhưng có lỗi khi gửi email. Chúng tôi sẽ liên hệ với bạn sớm nhất.";
    public static final String ERROR_MESSAGE = "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.";
    public static final String SERVICE_ACTIVE_MESSAGE = "Dịch vụ đăng ký đang hoạt động bình thường";

    // Service Status
    public static final String SERVICE_STATUS_ACTIVE = "ACTIVE";

    // Date Format
    public static final String REQUEST_ID_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS";
    public static final String DISPLAY_DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";
}
