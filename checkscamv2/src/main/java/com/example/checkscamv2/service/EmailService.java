package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.request.ServiceRegistrationRequest;
import com.example.checkscamv2.dto.request.TransactionRequestDTO;

public interface EmailService {
    void sendEmail(String to, String subject, String content);

    /**
     * Gửi email thông báo giao dịch mới cho giao dịch viên
     * @param request thông tin giao dịch
     * @param transactionId mã giao dịch
     * @return true nếu gửi thành công
     */
    boolean sendTransactionNotification(TransactionRequestDTO request, String transactionId);

    /**
     * Gửi email xác nhận cho hai bên tham gia giao dịch
     * @param request thông tin giao dịch
     * @param transactionId mã giao dịch
     * @return true nếu gửi thành công cho cả hai bên
     */
    boolean sendConfirmationEmails(TransactionRequestDTO request, String transactionId);

    /**
     * Gửi email xác nhận cho một bên cụ thể
     * @param request thông tin giao dịch
     * @param transactionId mã giao dịch
     * @param isPartyA true nếu là bên A, false nếu là bên B
     * @return true nếu gửi thành công
     */
    boolean sendPartyConfirmationEmail(TransactionRequestDTO request, String transactionId, boolean isPartyA);

    /**
     * Gửi email thông báo thay đổi trạng thái giao dịch
     * @param request thông tin giao dịch
     * @param transactionId mã giao dịch
     * @param oldStatus trạng thái cũ
     * @param newStatus trạng thái mới
     * @return true nếu gửi thành công
     */
    boolean sendStatusUpdateNotification(TransactionRequestDTO request, String transactionId,
                                         String oldStatus, String newStatus);

    /**
     * Gửi email thông báo đăng ký dịch vụ từ khách hàng
     * @param request thông tin đăng ký dịch vụ
     * @return true nếu gửi thành công
     */
    boolean sendServiceRegistrationNotification(ServiceRegistrationRequest request);

    /**
     * Gửi email xác nhẫn đăng ký thành công đến khách hàng
     * @param request thông tin đăng ký dịch vụ
     * @return true nếu gửi thành công
     */
    boolean sendServiceRegistrationConfirmation(ServiceRegistrationRequest request);
}