package com.example.checkscamv2.service;

/**
 * Enum định nghĩa các loại template email
 */
public enum EmailTemplate {
    TRANSACTION_NOTIFICATION("Thông báo giao dịch mới", "transaction_notification.html"),
    CONFIRMATION_EMAIL("Email xác nhận giao dịch", "confirmation_email.html"),
    STATUS_UPDATE("Thông báo cập nhật trạng thái", "status_update.html"),
    SERVICE_REGISTRATION_NOTIFICATION("Thông báo đăng ký dịch vụ", "service_registration_notification.html"),
    SERVICE_REGISTRATION_CONFIRMATION("Xác nhận đăng ký dịch vụ", "service_registration_confirmation.html");

    private final String subject;
    private final String templateFile;

    EmailTemplate(String subject, String templateFile) {
        this.subject = subject;
        this.templateFile = templateFile;
    }

    public String getSubject() {
        return subject;
    }

    public String getTemplateFile() {
        return templateFile;
    }
}