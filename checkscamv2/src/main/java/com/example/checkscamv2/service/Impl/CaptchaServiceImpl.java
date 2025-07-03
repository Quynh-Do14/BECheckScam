package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.dto.response.CaptchaResponse;
import com.example.checkscamv2.service.CaptchaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class CaptchaServiceImpl implements CaptchaService {

    @Value("${google.recaptcha.secret}")
    private String secretKey;

    @Value("${google.recaptcha.verify-url}")
    private String verifyUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean verify(String token) {
        // 1. Chuẩn bị tham số request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", secretKey);
        params.add("response", token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        // 2. Gửi POST đến Google
        ResponseEntity<CaptchaResponse> responseEntity =
                restTemplate.exchange(verifyUrl, HttpMethod.POST, request, CaptchaResponse.class);

        CaptchaResponse captchaResponse = responseEntity.getBody();
        return captchaResponse != null && captchaResponse.isSuccess();
    }
}
