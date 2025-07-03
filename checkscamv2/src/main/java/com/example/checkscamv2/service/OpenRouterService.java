package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.request.OpenRouterRequest;
import com.example.checkscamv2.dto.response.OpenRouterResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface OpenRouterService {
    OpenRouterResponse callOpenRouterApi(OpenRouterRequest request) throws JsonProcessingException;

    String analyzeScreenShot(String base64Image);

    String analyzeScamData(String data);
    String chatbot(String data);

}