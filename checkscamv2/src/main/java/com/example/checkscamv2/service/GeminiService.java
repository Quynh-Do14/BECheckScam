package com.example.checkscamv2.service;

public interface GeminiService {
    String chatbot(String message);
    String analyzeScreenShot(String base64Image);
    String analyzeScamData(String data);
}