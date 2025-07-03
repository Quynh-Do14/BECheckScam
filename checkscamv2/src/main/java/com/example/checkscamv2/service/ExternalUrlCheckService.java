package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.response.ExternalUrlCheckResponse;

import java.util.List;

public interface ExternalUrlCheckService {
    ExternalUrlCheckResponse checkUrlWithGoogleSafeBrowse(String url);
    ExternalUrlCheckResponse checkUrlWithGoogleWebRisk(String url);
    ExternalUrlCheckResponse checkUrlWithPhishTank(String url);
    ExternalUrlCheckResponse checkUrlWithVirusTotal(String url);
    ExternalUrlCheckResponse checkUrlWithUrlScan(String url);
    List<ExternalUrlCheckResponse> checkUrlWithAllServices(String url);
}