package com.example.checkscamv2.exception;


/**
 * Custom exception cho Transaction Service
 */
public class TransactionException extends RuntimeException {

    private String errorCode;
    private Object details;

    public TransactionException(String message) {
        super(message);
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransactionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TransactionException(String errorCode, String message, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getDetails() {
        return details;
    }

    // Specific exception types
    public static class TransactionNotFoundException extends TransactionException {
        public TransactionNotFoundException(String transactionId) {
            super("TRANSACTION_NOT_FOUND", "Không tìm thấy giao dịch với ID: " + transactionId);
        }
    }

    public static class InvalidTransactionDataException extends TransactionException {
        public InvalidTransactionDataException(String message) {
            super("INVALID_DATA", message);
        }

        public InvalidTransactionDataException(String message, Object details) {
            super("INVALID_DATA", message, details);
        }
    }

    public static class EmailSendingException extends TransactionException {
        public EmailSendingException(String message) {
            super("EMAIL_SENDING_FAILED", message);
        }

        public EmailSendingException(String message, Throwable cause) {
            super("EMAIL_SENDING_FAILED", message, cause);
        }
    }

    public static class DatabaseException extends TransactionException {
        public DatabaseException(String message) {
            super("DATABASE_ERROR", message);
        }

        public DatabaseException(String message, Throwable cause) {
            super("DATABASE_ERROR", message, cause);
        }
    }
}
