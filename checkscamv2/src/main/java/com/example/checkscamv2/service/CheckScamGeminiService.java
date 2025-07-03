package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.request.CheckScamRequest;
import com.example.checkscamv2.dto.response.ScamAnalysisResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface CheckScamGeminiService {
    ScamAnalysisResponse checkScam(CheckScamRequest request) throws JsonProcessingException;

    String analyzeScamData(String data);

    String analyzeScreenShot(String base64Image);

    String chatWithAI(String message);
}
