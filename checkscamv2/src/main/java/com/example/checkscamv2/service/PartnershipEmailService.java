package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.request.PartnershipRequest;

public interface PartnershipEmailService {
    
    /**
     * Gửi email thông báo đăng ký hợp tác đến admin
     * @param request thông tin đăng ký hợp tác
     * @return true nếu gửi thành công
     */
    boolean sendPartnershipNotificationToAdmin(PartnershipRequest request);
    
    /**
     * Gửi email xác nhận đăng ký hợp tác đến client
     * @param request thông tin đăng ký hợp tác
     * @return true nếu gửi thành công
     */
    boolean sendPartnershipConfirmationToClient(PartnershipRequest request);
    
    /**
     * Gửi email cho cả client và admin bất đồng bộ
     * @param request thông tin đăng ký hợp tác
     */
    void sendPartnershipEmails(PartnershipRequest request);
}