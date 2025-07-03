package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.constant.Constant;
import com.example.checkscamv2.dto.request.GeminiRequest;
import com.example.checkscamv2.dto.response.GeminiResponse;
import com.example.checkscamv2.service.GeminiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements GeminiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.timeout}")
    private int timeout;

    @Value("${gemini.max-retries}")
    private int maxRetries;

    @Override
    public String chatbot(String message) {
        try {
            GeminiRequest request = createRequest(message);
            HttpEntity<GeminiRequest> entity = createEntity(request);
            String response = callGeminiApi(entity);
            return extractTextResponse(response);
        } catch (Exception e) {
            log.error("Error calling Gemini API for chatbot: {}", e.getMessage());
            return "Xin lỗi, không thể xử lý yêu cầu của bạn.";
        }
    }

    @Override
    public String analyzeScreenShot(String base64Image) {
        try {
            GeminiRequest.Part.InlineData inlineData = GeminiRequest.Part.InlineData.builder()
                    .mimeType("image/png")
                    .data(base64Image)
                    .build();
            GeminiRequest.Part imagePart = GeminiRequest.Part.builder()
                    .inlineData(inlineData)
                    .build();
            GeminiRequest request = createRequest(Constant.PROMPT_SCREENSHOT_ANALYSIS, imagePart);
            HttpEntity<GeminiRequest> entity = createEntity(request);
            String response = callGeminiApi(entity);
            return extractTextResponse(response);
        } catch (Exception e) {
            log.error("Error analyzing screenshot with Gemini API: {}", e.getMessage());
            return "Không thể phân tích ảnh giao diện.";
        }
    }

    @Override
    public String analyzeScamData(String data) {
        try {
            GeminiRequest request = createRequest(data);
            HttpEntity<GeminiRequest> entity = createEntity(request);
            String response = callGeminiApi(entity);
            return extractTextResponse(response);
        } catch (Exception e) {
            log.error("Error analyzing scam data with Gemini API: {}", e.getMessage());
            return "Không thể phân tích thông tin lừa đảo do lỗi hệ thống.";
        }
    }

    private GeminiRequest createRequest(String text) {
        return createRequest(text, null);
    }

    private GeminiRequest createRequest(String text, GeminiRequest.Part imagePart) {
        GeminiRequest.Content content = GeminiRequest.Content.builder()
                .parts(imagePart != null ? List.of(GeminiRequest.Part.builder().text(text).build(), imagePart) : List.of(GeminiRequest.Part.builder().text(text).build()))
                .build();
        return GeminiRequest.builder()
                .contents(Collections.singletonList(content))
                .build();
    }

    private HttpEntity<GeminiRequest> createEntity(GeminiRequest request) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBodyJson = objectMapper.writeValueAsString(request);
        log.debug("Gemini API Request: {}", requestBodyJson);
        return new HttpEntity<>(request, headers);
    }

    private String callGeminiApi(HttpEntity<GeminiRequest> entity) {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetries) {
            try {
                String urlWithKey = apiUrl + "?key=" + apiKey;
                ResponseEntity<String> response = restTemplate.postForEntity(
                        urlWithKey, entity, String.class
                );

                log.debug("Raw Gemini API Response: {}", response.getBody());

                if (response.getStatusCode() == HttpStatus.OK) {
                    if (response.getBody() == null) {
                        log.warn("Empty response body from Gemini API");
                        throw new RuntimeException("Empty response body from Gemini API");
                    }

                    if (response.getBody().contains("\"error\"")) {
                        log.error("Gemini API returned an error: {}", response.getBody());
                        throw new RuntimeException("Gemini API error: " + response.getBody());
                    }

                    return response.getBody();
                } else {
                    log.warn("Gemini API returned status: {}", response.getStatusCode());
                    throw new RuntimeException("Unexpected status: " + response.getStatusCode());
                }
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
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        log.error("All retry attempts failed", lastException);
        throw new RuntimeException("Failed to call Gemini API after " + maxRetries + " attempts", lastException);
    }

    private String extractTextResponse(String response) {
        try {
            GeminiResponse geminiResponse = objectMapper.readValue(response, GeminiResponse.class);
            if (geminiResponse.getCandidates() == null || geminiResponse.getCandidates().isEmpty()) {
                log.warn("No candidates found in Gemini response: {}", response);
                return "Không nhận được phản hồi hợp lệ từ hệ thống phân tích.";
            }
            GeminiResponse.Candidate candidate = geminiResponse.getCandidates().get(0);
            if (candidate.getContent() == null || candidate.getContent().getParts() == null || candidate.getContent().getParts().isEmpty()) {
                log.warn("No content or parts found in Gemini response: {}", response);
                return "Không nhận được phản hồi hợp lệ từ hệ thống phân tích.";
            }
            return candidate.getContent().getParts().get(0).getText();
        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
            return "Không nhận được phản hồi từ hệ thống phân tích.";
        }
    }
}