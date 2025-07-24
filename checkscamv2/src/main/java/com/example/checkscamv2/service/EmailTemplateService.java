package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.request.ServiceRegistrationRequest;

/**
 * Service for building email templates
 * 
 * @author CheckScam Team
 */
public interface EmailTemplateService {

    /**
     * Build email content for admin notification
     * @param request service registration request
     * @return email content for admin
     */
    String buildAdminNotificationEmail(ServiceRegistrationRequest request);

    /**
     * Build email content for customer confirmation
     * @param request service registration request  
     * @return email content for customer
     */
    String buildCustomerConfirmationEmail(ServiceRegistrationRequest request);
}
