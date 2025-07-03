package com.example.checkscamv2.exception;

public class InvalidCaptchaException extends RuntimeException {
    public InvalidCaptchaException(String s) {
        super("CAPTCHA validation failed");
    }
}
