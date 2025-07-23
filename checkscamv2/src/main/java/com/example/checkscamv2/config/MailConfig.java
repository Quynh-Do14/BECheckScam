package com.example.checkscamv2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Bean
    @Primary  // Ưu tiên bean này thay vì auto-configuration
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // Cấu hình cơ bản
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);  // ✅ Force set password

        // Cấu hình SMTP properties
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        // Timeout settings
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        // Debug (có thể tắt sau khi fix)
        props.put("mail.debug", "false");

        return mailSender;
    }

    // Bean để debug config values
    @Bean
    public MailConfigChecker mailConfigChecker() {
        return new MailConfigChecker(host, port, username, password);
    }

    public static class MailConfigChecker {
        private final String host;
        private final int port;
        private final String username;
        private final String password;

        public MailConfigChecker(String host, int port, String username, String password) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;

            // Log config khi tạo bean
            System.out.println("=== MAIL CONFIG DEBUG ===");
            System.out.println("Host: " + host);
            System.out.println("Port: " + port);
            System.out.println("Username: " + username);
            System.out.println("Password exists: " + (password != null && !password.isEmpty()));
            System.out.println("Password length: " + (password != null ? password.length() : 0));
            System.out.println("==========================");
        }

        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getUsername() { return username; }
        public boolean hasPassword() { return password != null && !password.isEmpty(); }
    }
}