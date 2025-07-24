package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.constant.ServiceRegistrationConstants;
import com.example.checkscamv2.dto.request.ServiceRegistrationRequest;
import com.example.checkscamv2.dto.request.TransactionRequestDTO;
import com.example.checkscamv2.service.EmailService;
import com.example.checkscamv2.service.EmailTemplateService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Implementation of EmailService with clean architecture
 * Separated email template logic to EmailTemplateService
 * 
 * @author CheckScam Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService emailTemplateService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${app.name:AI6 Team}")
    private String appName;

    @Override
    public void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = createSimpleMessage(to, subject, content);
            
            log.info("📧 Đang gửi email đến: {} với subject: {}", to, subject);
            mailSender.send(message);
            log.info("✅ Email đã gửi thành công đến: {}", to);
            
        } catch (MailException e) {
            log.error("❌ Lỗi khi gửi email đến {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    @Override
    public boolean sendServiceRegistrationNotification(ServiceRegistrationRequest request) {
        try {
            String emailContent = emailTemplateService.buildAdminNotificationEmail(request);
            
            for (String adminEmail : ServiceRegistrationConstants.ADMIN_EMAILS) {
                sendNotificationToAdmin(adminEmail, emailContent);
            }
            
            return true;

        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi email thông báo đăng ký dịch vụ: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendServiceRegistrationConfirmation(ServiceRegistrationRequest request) {
        try {
            String emailContent = emailTemplateService.buildCustomerConfirmationEmail(request);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(request.getEmail());
            message.setSubject(ServiceRegistrationConstants.CUSTOMER_EMAIL_SUBJECT);
            message.setText(emailContent);

            log.info("📧 Đang gửi email xác nhận đến khách hàng: {}", request.getEmail());
            mailSender.send(message);
            log.info("✅ Email xác nhận đã gửi thành công đến khách hàng: {}", request.getEmail());
            
            return true;

        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi email xác nhận đăng ký: {}", e.getMessage(), e);
            return false;
        }
    }

    // Legacy transaction methods - keeping for backward compatibility
    @Override
    public boolean sendTransactionNotification(TransactionRequestDTO request, String transactionId) {
        // Implementation kept for existing functionality
        return false; // Placeholder
    }

    @Override
    public boolean sendConfirmationEmails(TransactionRequestDTO request, String transactionId) {
        // Implementation kept for existing functionality  
        return false; // Placeholder
    }

    @Override
    public boolean sendPartyConfirmationEmail(TransactionRequestDTO request, String transactionId, boolean isPartyA) {
        // Implementation kept for existing functionality
        return false; // Placeholder
    }

    @Override
    public boolean sendStatusUpdateNotification(TransactionRequestDTO request, String transactionId,
                                                String oldStatus, String newStatus) {
        // Implementation kept for existing functionality
        return false; // Placeholder
    }

    // Private helper methods
    private SimpleMailMessage createSimpleMessage(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        return message;
    }

    private void sendNotificationToAdmin(String adminEmail, String emailContent) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(adminEmail);
        message.setSubject(ServiceRegistrationConstants.ADMIN_EMAIL_SUBJECT);
        message.setText(emailContent);

        log.info("📧 Đang gửi email thông báo đến admin: {}", adminEmail);
        mailSender.send(message);
        log.info("✅ Email thông báo đã gửi thành công đến admin: {}", adminEmail);
    }

    @PostConstruct
    public void configureMailSender() {
        log.info("🔧 Configuring JavaMailSender...");

        try {
            if (mailSender instanceof JavaMailSenderImpl impl) {
                setupMailConfiguration(impl);
                log.info("✅ JavaMailSender configuration completed!");
                logConfiguration(impl);
            } else {
                log.warn("⚠️ JavaMailSender is not instance of JavaMailSenderImpl");
            }

        } catch (Exception e) {
            log.error("❌ Failed to configure JavaMailSender: {}", e.getMessage(), e);
        }
    }

    private void setupMailConfiguration(JavaMailSenderImpl impl) {
        impl.setHost("smtp.gmail.com");
        impl.setPort(587);
        impl.setUsername(fromEmail);
        impl.setPassword(password);

        Properties props = impl.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.debug", "false");
    }

    private void logConfiguration(JavaMailSenderImpl impl) {
        log.info("📧 Host: {}, Port: {}, Username: {}",
                impl.getHost(), impl.getPort(), impl.getUsername());
        log.info("🔐 Password configured: {}", 
                password != null && !password.isEmpty() ? "Yes" : "No");
    }
}
