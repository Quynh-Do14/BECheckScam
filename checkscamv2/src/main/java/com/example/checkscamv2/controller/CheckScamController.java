package com.example.checkscamv2.controller;

import com.example.checkscamv2.dto.request.ChatbotRequest;
import com.example.checkscamv2.dto.request.CheckScamRequest;
import com.example.checkscamv2.dto.request.UrlCheckRequest;
import com.example.checkscamv2.dto.response.ChatbotResponse;
import com.example.checkscamv2.dto.response.ExternalUrlCheckResponse;
import com.example.checkscamv2.dto.response.ScamAnalysisResponse;
import com.example.checkscamv2.service.*;
import com.example.checkscamv2.util.FileCacheUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/v1/check-scam")
@RequiredArgsConstructor
public class CheckScamController {
    private final CheckScamService checkScamService;
    private final PlaywrightService playwrightService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ScamStatsService scamStatsService;
    private final ExternalUrlCheckService externalUrlCheckService;
    private final CheckScamGeminiService checkScamGeminiService;

    @PostMapping
    public ResponseEntity<?> checkScam(@RequestBody CheckScamRequest request) throws JsonProcessingException {
        ScamAnalysisResponse response = checkScamGeminiService.checkScam(request);
        
        if (response != null && response.getInfo() != null) {
            try {
                incrementViewCount(response.getInfo(), response.getType());
            } catch (Exception e) {
                System.err.println("Error incrementing view count: " + e.getMessage());
            }
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Helper method để tăng view count
     */
    private void incrementViewCount(String info, Integer type) {
        try {
            switch (type) {
                case 1: // Phone
                    scamStatsService.incrementPhoneViewCount(info);
                    break;
                case 2: // Bank
                    scamStatsService.incrementBankViewCount(info);
                    break;
                case 3: // URL
                    scamStatsService.incrementUrlViewCount(info);
                    break;
                default:
                    System.err.println("Unknown type for view count increment: " + type);
            }
        } catch (Exception e) {
            System.err.println("Failed to increment view count for " + info + ": " + e.getMessage());
        }
    }

    @PostMapping("/all")
    public ResponseEntity<List<ExternalUrlCheckResponse>> checkWithAllServices(@RequestBody UrlCheckRequest request) {
        List<ExternalUrlCheckResponse> responses = externalUrlCheckService.checkUrlWithAllServices(request.getUrl());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeScamData(@RequestBody String data) {
        String result = checkScamService.analyzeScamData(data);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/chatbot")
    public ResponseEntity<ChatbotResponse> chatBot(@RequestBody ChatbotRequest request) {
        String response = checkScamGeminiService.chatWithAI(request.getMessage());
        return ResponseEntity.ok(ChatbotResponse.builder().response(response).build());
    }

    @GetMapping("/cache/{filename:.+}")
    public ResponseEntity<FileSystemResource> getImage(@PathVariable String filename) {
        File file = new File("cache/" + filename);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(new FileSystemResource(file));
    }
}