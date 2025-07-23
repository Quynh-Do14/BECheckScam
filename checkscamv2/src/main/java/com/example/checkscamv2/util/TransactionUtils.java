package com.example.checkscamv2.util;


import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Utility class cho Transaction Service
 */
public class TransactionUtils {

    // Constants
    public static final String TRANSACTION_ID_PREFIX = "TXN";
    public static final String DEFAULT_STATUS = "PENDING";

    // Patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10,11}$");
    private static final Pattern TRANSACTION_ID_PATTERN = Pattern.compile(
            "^" + TRANSACTION_ID_PREFIX + "-\\d{5}-[A-Z0-9]{4}$"
    );

    // Formatters
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        return StringUtils.hasText(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate phone number format
     */
    public static boolean isValidPhone(String phone) {
        return StringUtils.hasText(phone) && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Validate transaction ID format
     */
    public static boolean isValidTransactionId(String transactionId) {
        return StringUtils.hasText(transactionId) &&
                TRANSACTION_ID_PATTERN.matcher(transactionId).matches();
    }

    /**
     * Validate transaction status
     */
    public static boolean isValidStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }

        String upperStatus = status.toUpperCase();
        return "PENDING".equals(upperStatus) ||
                "IN_PROGRESS".equals(upperStatus) ||
                "COMPLETED".equals(upperStatus) ||
                "CANCELLED".equals(upperStatus) ||
                "DELETED".equals(upperStatus);
    }

    /**
     * Format datetime to string
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : "";
    }

    /**
     * Format date to string
     */
    public static String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : "";
    }

    /**
     * Normalize phone number (remove spaces, dashes, etc.)
     */
    public static String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return "";
        }

        return phone.replaceAll("[^0-9]", "");
    }

    /**
     * Normalize email (trim and lowercase)
     */
    public static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "";
        }

        return email.trim().toLowerCase();
    }

    /**
     * Sanitize string input (trim and remove dangerous characters)
     */
    public static String sanitizeString(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }

        return input.trim()
                .replaceAll("[<>\"'&]", "") // Remove potential XSS characters
                .replaceAll("\\s+", " ");   // Normalize whitespace
    }

    /**
     * Mask email for privacy (show only first 2 chars and domain)
     */
    public static String maskEmail(String email) {
        if (!isValidEmail(email)) {
            return "";
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            return "";
        }

        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return username + "@" + domain;
        }

        return username.substring(0, 2) + "*".repeat(username.length() - 2) + "@" + domain;
    }

    /**
     * Mask phone number for privacy (show only last 3 digits)
     */
    public static String maskPhone(String phone) {
        if (!isValidPhone(phone)) {
            return "";
        }

        if (phone.length() <= 3) {
            return phone;
        }

        return "*".repeat(phone.length() - 3) + phone.substring(phone.length() - 3);
    }

    /**
     * Generate email subject for transaction
     */
    public static String generateEmailSubject(String type, String transactionId) {
        switch (type.toUpperCase()) {
            case "CREATE":
                return "🔔 Yêu cầu giao dịch mới - " + transactionId;
            case "CONFIRM":
                return "✅ Xác nhận yêu cầu giao dịch - " + transactionId;
            case "UPDATE":
                return "🔄 Cập nhật trạng thái giao dịch - " + transactionId;
            case "COMPLETE":
                return "🎉 Giao dịch hoàn thành - " + transactionId;
            case "CANCEL":
                return "❌ Giao dịch đã hủy - " + transactionId;
            default:
                return "📧 Thông báo giao dịch - " + transactionId;
        }
    }

    /**
     * Get status display text in Vietnamese
     */
    public static String getStatusDisplayText(String status) {
        if (!StringUtils.hasText(status)) {
            return "";
        }

        switch (status.toUpperCase()) {
            case "PENDING":
                return "Chờ xử lý";
            case "IN_PROGRESS":
                return "Đang xử lý";
            case "COMPLETED":
                return "Đã hoàn thành";
            case "CANCELLED":
                return "Đã hủy";
            case "DELETED":
                return "Đã xóa";
            default:
                return status;
        }
    }

    /**
     * Get status emoji
     */
    public static String getStatusEmoji(String status) {
        if (!StringUtils.hasText(status)) {
            return "❓";
        }

        switch (status.toUpperCase()) {
            case "PENDING":
                return "⏳";
            case "IN_PROGRESS":
                return "🔄";
            case "COMPLETED":
                return "✅";
            case "CANCELLED":
                return "❌";
            case "DELETED":
                return "🗑️";
            default:
                return "❓";
        }
    }

    /**
     * Check if transaction is in final state
     */
    public static boolean isFinalStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }

        String upperStatus = status.toUpperCase();
        return "COMPLETED".equals(upperStatus) ||
                "CANCELLED".equals(upperStatus) ||
                "DELETED".equals(upperStatus);
    }

    /**
     * Validate room name format
     */
    public static boolean isValidRoomName(String roomName) {
        return StringUtils.hasText(roomName) &&
                roomName.length() >= 3 &&
                roomName.length() <= 100;
    }

    /**
     * Validate person name format
     */
    public static boolean isValidPersonName(String name) {
        return StringUtils.hasText(name) &&
                name.length() >= 2 &&
                name.length() <= 100 &&
                !name.matches(".*[0-9].*"); // No numbers in name
    }
}
