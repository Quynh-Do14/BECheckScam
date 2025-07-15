package com.example.checkscamv2.service;

public interface EmailService {
    void sendEmail(String to, String subject, String content);
}