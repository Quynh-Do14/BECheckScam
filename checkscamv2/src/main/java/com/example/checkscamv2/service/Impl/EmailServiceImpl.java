package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.constant.ServiceRegistrationConstants;
import com.example.checkscamv2.dto.request.ServiceRegistrationRequest;
import com.example.checkscamv2.dto.request.TransactionRequestDTO;
import com.example.checkscamv2.service.EmailService;
import com.example.checkscamv2.service.EmailTemplate;
import com.example.checkscamv2.service.EmailTemplateService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

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
    @Async
    public CompletableFuture<Boolean> sendEmailAsync(String to, String subject, String content) {
        try {
            SimpleMailMessage message = createSimpleMessage(to, subject, content);
            
            log.info("📧 [ASYNC] Đang gửi email đến: {} với subject: {}", to, subject);
            mailSender.send(message);
            log.info("✅ [ASYNC] Email đã gửi thành công đến: {}", to);
            
            return CompletableFuture.completedFuture(true);
            
        } catch (MailException e) {
            log.error("❌ [ASYNC] Lỗi khi gửi email đến {}: {}", to, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendEmailWithTemplate(String to, EmailTemplate template, Object data) {
        try {
            String content = buildEmailContent(template, data);
            String subject = buildEmailSubject(template, data);
            
            return sendEmailAsync(to, subject, content);
            
        } catch (Exception e) {
            log.error("❌ [ASYNC] Lỗi khi gửi email với template {} đến {}: {}", 
                     template.name(), to, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public boolean sendServiceRegistrationNotification(ServiceRegistrationRequest request) {
        try {
            for (String adminEmail : ServiceRegistrationConstants.ADMIN_EMAILS) {
                sendEmailWithTemplate(adminEmail, EmailTemplate.SERVICE_REGISTRATION_NOTIFICATION, request);
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
            sendEmailWithTemplate(request.getEmail(), EmailTemplate.SERVICE_REGISTRATION_CONFIRMATION, request);
            return true;

        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi email xác nhận đăng ký: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendTransactionNotification(TransactionRequestDTO request, String transactionId) {
        try {
            TransactionEmailData data = new TransactionEmailData(request, transactionId);
            // Gửi đến admin/giao dịch viên
            sendEmailWithTemplate("admin@checkscam.com", EmailTemplate.TRANSACTION_NOTIFICATION, data);
            return true;
        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi thông báo giao dịch: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendConfirmationEmails(TransactionRequestDTO request, String transactionId) {
        try {
            TransactionEmailData data = new TransactionEmailData(request, transactionId);
            // Gửi cho cả hai bên
            sendEmailWithTemplate(request.getPartyAEmail(), EmailTemplate.CONFIRMATION_EMAIL, data);
            sendEmailWithTemplate(request.getPartyBEmail(), EmailTemplate.CONFIRMATION_EMAIL, data);
            return true;
        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi email xác nhận: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendPartyConfirmationEmail(TransactionRequestDTO request, String transactionId, boolean isPartyA) {
        try {
            TransactionEmailData data = new TransactionEmailData(request, transactionId, isPartyA);
            String email = isPartyA ? request.getPartyAEmail() : request.getPartyBEmail();
            sendEmailWithTemplate(email, EmailTemplate.CONFIRMATION_EMAIL, data);
            return true;
        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi email xác nhận cho bên {}: {}", isPartyA ? "A" : "B", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendStatusUpdateNotification(TransactionRequestDTO request, String transactionId,
                                                String oldStatus, String newStatus) {
        try {
            StatusUpdateEmailData data = new StatusUpdateEmailData(request, transactionId, oldStatus, newStatus);
            // Gửi cho cả hai bên
            sendEmailWithTemplate(request.getPartyAEmail(), EmailTemplate.STATUS_UPDATE, data);
            sendEmailWithTemplate(request.getPartyBEmail(), EmailTemplate.STATUS_UPDATE, data);
            return true;
        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi thông báo cập nhật trạng thái: {}", e.getMessage(), e);
            return false;
        }
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

    private String buildEmailContent(EmailTemplate template, Object data) {
        switch (template) {
            case TRANSACTION_NOTIFICATION:
                return buildTransactionNotificationContent((TransactionEmailData) data);
            case CONFIRMATION_EMAIL:
                return buildConfirmationEmailContent((TransactionEmailData) data);
            case STATUS_UPDATE:
                return buildStatusUpdateContent((StatusUpdateEmailData) data);
            case SERVICE_REGISTRATION_NOTIFICATION:
                return emailTemplateService.buildAdminNotificationEmail((ServiceRegistrationRequest) data);
            case SERVICE_REGISTRATION_CONFIRMATION:
                return emailTemplateService.buildCustomerConfirmationEmail((ServiceRegistrationRequest) data);
            default:
                return "Email content";
        }
    }

    private String buildEmailSubject(EmailTemplate template, Object data) {
        switch (template) {
            case SERVICE_REGISTRATION_NOTIFICATION:
                return ServiceRegistrationConstants.ADMIN_EMAIL_SUBJECT;
            case SERVICE_REGISTRATION_CONFIRMATION:
                return ServiceRegistrationConstants.CUSTOMER_EMAIL_SUBJECT;
            default:
                return template.getSubject();
        }
    }

    private String buildTransactionNotificationContent(TransactionEmailData data) {
        return String.format(
            "Thông báo giao dịch mới\n\n" +
            "Mã giao dịch: %s\n" +
            "Giao dịch viên: %s (%s)\n" +
            "Bên A: %s (%s)\n" +
            "Bên B: %s (%s)\n" +
            "Tên phòng: %s\n\n" +
            "Vui lòng kiểm tra và xử lý giao dịch.",
            data.getTransactionId(),
            data.getRequest().getDealerName(),
            data.getRequest().getDealerEmail(),
            data.getRequest().getPartyAName(),
            data.getRequest().getPartyAEmail(),
            data.getRequest().getPartyBName(),
            data.getRequest().getPartyBEmail(),
            data.getRequest().getRoomName()
        );
    }

    private String buildConfirmationEmailContent(TransactionEmailData data) {
        return String.format(
            "Xác nhận giao dịch\n\n" +
            "Mã giao dịch: %s\n" +
            "Giao dịch viên: %s\n" +
            "Tên phòng: %s\n" +
            "Mã giao dịch: %s\n\n" +
            "Giao dịch của bạn đã được tạo thành công.",
            data.getTransactionId(),
            data.getRequest().getDealerName(),
            data.getRequest().getRoomName(),
            data.getRequest().getTransactionCode()
        );
    }

    private String buildStatusUpdateContent(StatusUpdateEmailData data) {
        return String.format(
            "Cập nhật trạng thái giao dịch\n\n" +
            "Mã giao dịch: %s\n" +
            "Trạng thái cũ: %s\n" +
            "Trạng thái mới: %s\n\n" +
            "Giao dịch của bạn đã được cập nhật.",
            data.getTransactionId(),
            data.getOldStatus(),
            data.getNewStatus()
        );
    }

    // Data classes for email templates
    public static class TransactionEmailData {
        private final TransactionRequestDTO request;
        private final String transactionId;
        private final Boolean isPartyA;

        public TransactionEmailData(TransactionRequestDTO request, String transactionId) {
            this(request, transactionId, null);
        }

        public TransactionEmailData(TransactionRequestDTO request, String transactionId, Boolean isPartyA) {
            this.request = request;
            this.transactionId = transactionId;
            this.isPartyA = isPartyA;
        }

        public TransactionRequestDTO getRequest() { return request; }
        public String getTransactionId() { return transactionId; }
        public Boolean getIsPartyA() { return isPartyA; }
    }

    public static class StatusUpdateEmailData {
        private final TransactionRequestDTO request;
        private final String transactionId;
        private final String oldStatus;
        private final String newStatus;

        public StatusUpdateEmailData(TransactionRequestDTO request, String transactionId, 
                                   String oldStatus, String newStatus) {
            this.request = request;
            this.transactionId = transactionId;
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
        }

        public TransactionRequestDTO getRequest() { return request; }
        public String getTransactionId() { return transactionId; }
        public String getOldStatus() { return oldStatus; }
        public String getNewStatus() { return newStatus; }
    }
}
