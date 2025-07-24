
package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.dto.request.TransactionRequestDTO;
import com.example.checkscamv2.service.EmailService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${app.name:AI6 Team}")
    private String appName;



    @Override
    public void sendEmail(String to, String subject, String content) {
        try {

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("fromEmail");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            System.out.println("Email sent successfully to: " + to);
        } catch (MailException e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    @Override
    public boolean sendTransactionNotification(TransactionRequestDTO request, String transactionId) {
        try {

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(request.getDealerEmail());
            message.setSubject("🔔 Yêu cầu giao dịch mới - " + transactionId);
            message.setText(buildTransactionEmailContent(request, transactionId));

            mailSender.send(message);
            log.info("Email đã được gửi thành công cho giao dịch viên: {} - Transaction: {}",
                    request.getDealerEmail(), transactionId);
            return true;

        } catch (Exception e) {
            log.error("Lỗi khi gửi email cho transaction {}: {}", transactionId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendConfirmationEmails(TransactionRequestDTO request, String transactionId) {
        boolean partyASent = sendPartyConfirmationEmail(request, transactionId, true);
        boolean partyBSent = sendPartyConfirmationEmail(request, transactionId, false);

        return partyASent && partyBSent;
    }

    @Override
    public boolean sendPartyConfirmationEmail(TransactionRequestDTO request, String transactionId, boolean isPartyA) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);

            if (isPartyA) {
                message.setTo(request.getPartyAEmail());
            } else {
                message.setTo(request.getPartyBEmail());
            }

            message.setSubject("✅ Xác nhận yêu cầu giao dịch - " + transactionId);
            message.setText(buildPartyConfirmationEmailContent(request, transactionId, isPartyA));

            mailSender.send(message);
            log.info("Email xác nhận đã được gửi cho {}: {}",
                    isPartyA ? "Party A" : "Party B",
                    isPartyA ? request.getPartyAEmail() : request.getPartyBEmail());
            return true;

        } catch (Exception e) {
            log.error("Lỗi khi gửi email xác nhận: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendStatusUpdateNotification(TransactionRequestDTO request, String transactionId,
                                                String oldStatus, String newStatus) {
        try {
            // Gửi cho giao dịch viên
            SimpleMailMessage dealerMessage = new SimpleMailMessage();
            dealerMessage.setFrom(fromEmail);
            dealerMessage.setTo(request.getDealerEmail());
            dealerMessage.setSubject("🔄 Cập nhật trạng thái giao dịch - " + transactionId);
            dealerMessage.setText(buildStatusUpdateEmailContent(request, transactionId, oldStatus, newStatus, true));

            // Gửi cho hai bên
            SimpleMailMessage partyAMessage = new SimpleMailMessage();
            partyAMessage.setFrom(fromEmail);
            partyAMessage.setTo(request.getPartyAEmail());
            partyAMessage.setSubject("🔄 Cập nhật trạng thái giao dịch - " + transactionId);
            partyAMessage.setText(buildStatusUpdateEmailContent(request, transactionId, oldStatus, newStatus, false));

            SimpleMailMessage partyBMessage = new SimpleMailMessage();
            partyBMessage.setFrom(fromEmail);
            partyBMessage.setTo(request.getPartyBEmail());
            partyBMessage.setSubject("🔄 Cập nhật trạng thái giao dịch - " + transactionId);
            partyBMessage.setText(buildStatusUpdateEmailContent(request, transactionId, oldStatus, newStatus, false));

            mailSender.send(dealerMessage);
            mailSender.send(partyAMessage);
            mailSender.send(partyBMessage);

            log.info("Email cập nhật trạng thái đã được gửi cho transaction: {}", transactionId);
            return true;

        } catch (Exception e) {
            log.error("Lỗi khi gửi email cập nhật trạng thái: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Tạo nội dung email cho giao dịch viên
     */
    private String buildTransactionEmailContent(TransactionRequestDTO request, String transactionId) {
        StringBuilder content = new StringBuilder();

        content.append("Kính chào ").append(request.getDealerName()).append(",\n\n");
        content.append("Bạn có một yêu cầu giao dịch mới cần xử lý:\n\n");

        content.append("📋 THÔNG TIN GIAO DỊCH\n");
        content.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        content.append("• Mã giao dịch: ").append(transactionId).append("\n");
        content.append("• Phòng: ").append(request.getRoomName()).append("\n");
        content.append("• Thời gian: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n\n");

        content.append("👤 THÔNG TIN BÊN A (Người tạo giao dịch)\n");
        content.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        content.append("• Tên: ").append(request.getPartyAName()).append("\n");
        content.append("• Email: ").append(request.getPartyAEmail()).append("\n");
        content.append("• Số điện thoại: ").append(request.getPartyAPhone()).append("\n\n");

        content.append("👥 THÔNG TIN BÊN B\n");
        content.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        content.append("• Tên: ").append(request.getPartyBName()).append("\n");
        content.append("• Email: ").append(request.getPartyBEmail()).append("\n");
        content.append("• Số điện thoại: ").append(request.getPartyBPhone()).append("\n\n");

        content.append("📞 HÀNH ĐỘNG CẦN THỰC HIỆN\n");
        content.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        content.append("1. Liên hệ với bên A: ").append(request.getPartyAPhone()).append("\n");
        content.append("2. Liên hệ với bên B: ").append(request.getPartyBPhone()).append("\n");
        content.append("3. Sắp xếp cuộc gặp tại phòng: ").append(request.getRoomName()).append("\n");
        content.append("4. Tiến hành giao dịch theo quy trình chuẩn\n\n");

        content.append("⚠️ LƯU Ý QUAN TRỌNG\n");
        content.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        content.append("• Vui lòng liên hệ với hai bên trong vòng 24 giờ\n");
        content.append("• Xác minh danh tính trước khi tiến hành giao dịch\n");
        content.append("• Tuân thủ đầy đủ quy trình an toàn\n\n");

        content.append("Trân trọng,\n");
        content.append("Hệ thống ").append(appName).append("\n");
        content.append("📧 Email: ").append(fromEmail).append("\n");
        content.append("🕐 Thời gian gửi: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

        return content.toString();
    }

    /**
     * Tạo nội dung email xác nhận cho các bên
     */
    private String buildPartyConfirmationEmailContent(TransactionRequestDTO request, String transactionId, boolean isPartyA) {
        StringBuilder content = new StringBuilder();

        String partyName = isPartyA ? request.getPartyAName() : request.getPartyBName();
        String otherPartyName = isPartyA ? request.getPartyBName() : request.getPartyAName();
        String otherPartyPhone = isPartyA ? request.getPartyBPhone() : request.getPartyAPhone();

        content.append("Kính chào ").append(partyName).append(",\n\n");
        content.append("Yêu cầu giao dịch của bạn đã được tiếp nhận và xử lý:\n\n");

        content.append("📋 THÔNG TIN GIAO DỊCH\n");
        content.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        content.append("• Mã giao dịch: ").append(transactionId).append("\n");
        content.append("• Đối tác: ").append(otherPartyName).append("\n");
        content.append("• Số điện thoại: ").append(otherPartyPhone).append("\n");
        content.append("• Phòng: ").append(request.getRoomName()).append("\n");
        content.append("• Giao dịch viên: ").append(request.getDealerName()).append("\n\n");

        content.append("📞 THÔNG TIN LIÊN HỆ\n");
        content.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        content.append("• Giao dịch viên sẽ liên hệ với bạn trong vòng 24 giờ\n");
        content.append("• Email giao dịch viên: ").append(request.getDealerEmail()).append("\n\n");

        content.append("⚠️ LƯU Ý AN TOÀN\n");
        content.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        content.append("• Chỉ giao dịch tại địa điểm đã thỏa thuận\n");
        content.append("• Mang theo giấy tờ tùy thân hợp lệ\n");
        content.append("• Không chia sẻ thông tin cá nhân với bên thứ 3\n\n");

        content.append("Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!\n\n");
        content.append("Trân trọng,\n");
        content.append("Đội ngũ ").append(appName);

        return content.toString();
    }

    /**
     * Tạo nội dung email cập nhật trạng thái
     */
    private String buildStatusUpdateEmailContent(TransactionRequestDTO request, String transactionId,
                                                 String oldStatus, String newStatus, boolean isForDealer) {
        StringBuilder content = new StringBuilder();

        if (isForDealer) {
            content.append("Kính chào ").append(request.getDealerName()).append(",\n\n");
            content.append("Trạng thái giao dịch ").append(transactionId).append(" đã được cập nhật:\n\n");
        } else {
            content.append("Kính chào,\n\n");
            content.append("Trạng thái giao dịch của bạn đã được cập nhật:\n\n");
        }

        content.append("📋 THÔNG TIN CẬP NHẬT\n");
        content.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        content.append("• Mã giao dịch: ").append(transactionId).append("\n");
        content.append("• Trạng thái cũ: ").append(getStatusText(oldStatus)).append("\n");
        content.append("• Trạng thái mới: ").append(getStatusText(newStatus)).append("\n");
        content.append("• Thời gian cập nhật: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n\n");

        // Thêm hướng dẫn tùy theo trạng thái
        content.append(getStatusGuidance(newStatus));

        content.append("Trân trọng,\n");
        content.append("Hệ thống ").append(appName);

        return content.toString();
    }

    /**
     * Chuyển đổi status code thành text hiển thị
     */
    private String getStatusText(String status) {
        switch (status.toUpperCase()) {
            case "PENDING": return "Chờ xử lý";
            case "IN_PROGRESS": return "Đang xử lý";
            case "COMPLETED": return "Đã hoàn thành";
            case "CANCELLED": return "Đã hủy";
            default: return status;
        }
    }

    /**
     * Lấy hướng dẫn tùy theo trạng thái
     */
    private String getStatusGuidance(String status) {
        switch (status.toUpperCase()) {
            case "IN_PROGRESS":
                return "📝 Giao dịch đang được xử lý. Vui lòng chuẩn bị đầy đủ giấy tờ và có mặt đúng giờ.\n\n";
            case "COMPLETED":
                return "🎉 Giao dịch đã hoàn thành thành công. Cảm ơn bạn đã sử dụng dịch vụ!\n\n";
            case "CANCELLED":
                return "❌ Giao dịch đã bị hủy. Nếu có thắc mắc, vui lòng liên hệ bộ phận hỗ trợ.\n\n";
            default:
                return "\n";
        }
    }

    @PostConstruct
    public void fixMailSenderPassword() {
        log.info("🔧 Checking JavaMailSender configuration...");

        try {
            if (mailSender instanceof JavaMailSenderImpl) {
                JavaMailSenderImpl impl = (JavaMailSenderImpl) mailSender;

                // Kiểm tra password
                if (impl.getPassword() == null || impl.getPassword().isEmpty()) {
                    log.warn("⚠️ JavaMailSender missing password, applying fix...");

                    // Set lại password và config
                    impl.setHost("smtp.gmail.com");
                    impl.setPort(587);
                    impl.setUsername(fromEmail);
                    impl.setPassword(password);

                    // Set properties
                    Properties props = impl.getJavaMailProperties();
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
                    props.put("mail.transport.protocol", "smtp");

                    log.info("✅ JavaMailSender password and config fixed!");
                    log.info("📧 Host: {}, Port: {}, Username: {}",
                            impl.getHost(), impl.getPort(), impl.getUsername());

                } else {
                    log.info("✅ JavaMailSender already has password configured");
                }
            }

        } catch (Exception e) {
            log.error("❌ Failed to fix JavaMailSender: {}", e.getMessage(), e);
        }
    }

}