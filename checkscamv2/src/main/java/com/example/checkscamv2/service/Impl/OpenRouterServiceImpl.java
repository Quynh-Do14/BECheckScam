package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.constant.Constant;
import com.example.checkscamv2.dto.request.OpenRouterRequest;
import com.example.checkscamv2.dto.response.OpenRouterResponse;
import com.example.checkscamv2.service.OpenRouterService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenRouterServiceImpl implements OpenRouterService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openrouter.api.url}")
    private String apiUrl;

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.model}")
    private String model;

    @Value("${openrouter.temperature}")
    private double temperature;

    @Value("${openrouter.max-tokens}")
    private int maxTokens;

    @Value("${openrouter.timeout}")
    private int timeout;

    @Value("${openrouter.max-retries}")
    private int maxRetries;

    @Override
    public OpenRouterResponse callOpenRouterApi(OpenRouterRequest request) throws JsonProcessingException {
        HttpHeaders headers = createHeaders();
        String requestBody = objectMapper.writeValueAsString(request);
        log.debug("OpenRouter API Request: {}", requestBody);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        
        // 4. Gọi API với retry mechanism
        return executeWithRetry(entity);
    }

    @Override
    public String chatbot(String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", message)));
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            String response = restTemplate.postForObject(
                    apiUrl,
                    entity,
                    String.class
            );
            return parseChatResponse(response);
        } catch (Exception e) {
            log.error("Error calling OpenRouter API: {}", e.getMessage());
            return "Xin lỗi, không thể xử lý yêu cầu của bạn.";
        }
    }

    @Override
    public String analyzeScreenShot(String base64Image) {
        try {
            OpenRouterRequest.Message imageMessage = OpenRouterRequest.Message.builder()
                    .role("user")
                    .content(List.of(
                            Map.of("type", "image_url", "image_url", Map.of("url", "data:image/png;base64," + base64Image))
                    ))
                    .build();

            OpenRouterRequest.Message textMessage = OpenRouterRequest.Message.builder()
                    .role("user")
                    .content(Constant.PROMPT_SCREENSHOT_ANALYSIS)
                    .build();

            OpenRouterRequest request = OpenRouterRequest.builder()
                    .model(model)
                    .messages(List.of(imageMessage, textMessage))
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();

            OpenRouterResponse response = callOpenRouterApi(request);
            return extractContent(response);

        } catch (Exception e) {
            log.error("Error analyzing screenshot: {}", e.getMessage());
            return "Không thể phân tích ảnh giao diện.";
        }
    }


    private String parseChatResponse(String response) {
        return response.contains("content") ? response.split("\"content\":\"")[1].split("\"")[0] : "Không có phản hồi.";
    }

    @Override
    public String analyzeScamData(String data) {
        try {
            OpenRouterRequest.Message message = createMessage(data);
            OpenRouterRequest request = createRequest(message);

            OpenRouterResponse response = callOpenRouterApi(request);
            return extractContent(response);
            
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON in analyzeScamData: {}", e.getMessage());
            return "Không thể phân tích thông tin lừa đảo do lỗi xử lý dữ liệu.";
        } catch (Exception e) {
            log.error("Error in analyzeScamData: {}", e.getMessage());
            return "Không thể phân tích thông tin lừa đảo do lỗi hệ thống.";
        }
    }


    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    private OpenRouterRequest.Message createMessage(String data) {
        return OpenRouterRequest.Message.builder()
                .role("user")
                .content(data)
                .build();
    }

    private OpenRouterRequest createRequest(OpenRouterRequest.Message message) {
        return OpenRouterRequest.builder()
                .model(model)
                .messages(Collections.singletonList(message))
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }

    private String extractContent(OpenRouterResponse response) {
        if (response != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getMessage().getContent();
        }
        return "Không nhận được phản hồi từ hệ thống phân tích.";
    }

    private OpenRouterResponse executeWithRetry(HttpEntity<String> entity) {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetries) {
            try {
                ResponseEntity<OpenRouterResponse> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    OpenRouterResponse.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    return response.getBody();
                }

                log.warn("OpenRouter API returned non-OK status: {}", response.getStatusCode());
                
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {} failed: {}", retryCount + 1, e.getMessage());
            }

            retryCount++;
            if (retryCount < maxRetries) {
                try {
                    Thread.sleep(1000L * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.error("All retry attempts failed", lastException);
        throw new RuntimeException("Failed to call OpenRouter API after " + maxRetries + " attempts", lastException);
    }
} 