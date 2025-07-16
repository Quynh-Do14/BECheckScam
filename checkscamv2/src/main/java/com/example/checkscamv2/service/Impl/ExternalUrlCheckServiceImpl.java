package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.dto.response.ExternalUrlCheckResponse;
import com.example.checkscamv2.service.ExternalUrlCheckService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
// import com.google.gson.JsonObject; // <-- XÓA DÒNG NÀY HOẶC COMMENT LẠI
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalUrlCheckServiceImpl implements ExternalUrlCheckService {

    @Value("${google.safe.Browse.api.key}")
    private String googleSafeBrowseApiKey;
    @Value("${google.safe.Browse.api.url}")
    private String googleSafeBrowseApiUrl;

    @Value("${google.web.risk.api.key}")
    private String googleWebRiskApiKey;
    @Value("${google.web.risk.api.url}")
    private String googleWebRiskApiUrl;

    @Value("${phishtank.api.key}")
    private String phishTankApiKey;
    @Value("${phishtank.api.url}")
    private String phishTankApiUrl;

    @Value("${virustotal.api.key}")
    private String virusTotalApiKey;
    @Value("${virustotal.api.url}")
    private String virusTotalApiUrl;

    @Value("${urlscan.api.key}")
    private String urlscanApiKey;
    @Value("${urlscan.api.url}")
    private String urlscanApiUrl;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Override
    public List<ExternalUrlCheckResponse> checkUrlWithAllServices(String url) {
//        CompletableFuture<ExternalUrlCheckResponse> googleSafeBrowse = CompletableFuture.supplyAsync(() -> checkUrlWithGoogleSafeBrowse(url));
        CompletableFuture<ExternalUrlCheckResponse> phishTank = CompletableFuture.supplyAsync(() -> checkUrlWithPhishTank(url));
        //tạm thời comment dòng này nhá, chưa mở thanh toán key nó log ra lỗi
//        CompletableFuture<ExternalUrlCheckResponse> googleWebRisk = CompletableFuture.supplyAsync(() -> checkUrlWithGoogleWebRisk(url));
        CompletableFuture<ExternalUrlCheckResponse> virusTotal = CompletableFuture.supplyAsync(() -> checkUrlWithVirusTotal(url));
        CompletableFuture<ExternalUrlCheckResponse> urlScan = CompletableFuture.supplyAsync(() -> checkUrlWithUrlScan(url));

        // Đảm bảo tất cả các CompletableFuture được bao gồm trong allOf
        return CompletableFuture.allOf(phishTank, virusTotal, urlScan)
                .thenApply(v -> Arrays.asList(
                        phishTank.join(),
                        virusTotal.join(),
                        urlScan.join()
                )).join();
    }

    @Override
    public ExternalUrlCheckResponse checkUrlWithGoogleSafeBrowse(String url) {
        String requestBody = createSafeBrowseRequestBody(url);
        String apiUrl = googleSafeBrowseApiUrl + "?key=" + googleSafeBrowseApiKey;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                boolean isMalicious = jsonNode.has("matches") && !jsonNode.get("matches").isEmpty();
                log.info("GoogleSafeBrowse kiểm tra URL {}: isMalicious={}", url, isMalicious);
                return new ExternalUrlCheckResponse("GoogleSafeBrowse", isMalicious);
            }
        } catch (HttpClientErrorException e) {
            log.error("Lỗi HTTP khi kiểm tra URL {} với GoogleSafeBrowse: status={}, body={}", url, e.getStatusCode(), e.getResponseBodyAsString());
            return new ExternalUrlCheckResponse("GoogleSafeBrowse", false);
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra URL {} với GoogleSafeBrowse: {}", url, e.getMessage());
            return new ExternalUrlCheckResponse("GoogleSafeBrowse", false);
        }
        return new ExternalUrlCheckResponse("GoogleSafeBrowse", false);
    }

    @Override
    public ExternalUrlCheckResponse checkUrlWithGoogleWebRisk(String url) {
        String apiUrl = googleWebRiskApiUrl + "?key=" + googleWebRiskApiKey +
                "&uri=" + URLEncoder.encode(url, StandardCharsets.UTF_8) +
                "&threatTypes=MALWARE&threatTypes=SOCIAL_ENGINEERING";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                boolean isMalicious = jsonNode.has("threat") && !jsonNode.get("threat").isEmpty();
                log.info("GoogleWebRisk kiểm tra URL {}: isMalicious={}", url, isMalicious);
                return new ExternalUrlCheckResponse("GoogleWebRisk", isMalicious);
            }
        } catch (HttpClientErrorException e) {
            log.error("Lỗi HTTP khi kiểm tra URL {} với GoogleWebRisk: status={}, body={}", url, e.getStatusCode(), e.getResponseBodyAsString());
            return new ExternalUrlCheckResponse("GoogleWebRisk", false);
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra URL {} với GoogleWebRisk: {}", url, e.getMessage());
            return new ExternalUrlCheckResponse("GoogleWebRisk", false);
        }
        return new ExternalUrlCheckResponse("GoogleWebRisk", false);
    }

    @Override
    public ExternalUrlCheckResponse checkUrlWithPhishTank(String url) {
        String apiUrl = phishTankApiUrl;
        String requestBody = String.format("url=%s&format=json&app_key=%s",
                URLEncoder.encode(url, StandardCharsets.UTF_8), phishTankApiKey);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            log.info("Gửi yêu cầu PhishTank cho URL: {}, body: {}", url, requestBody);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                boolean isMalicious = jsonNode.path("results").path("in_database").asBoolean(false) &&
                        jsonNode.path("results").path("verified").asBoolean(true) &&
                        jsonNode.path("results").path("valid").asBoolean(true);
                log.info("PhishTank kiểm tra URL {}: isMalicious={}", url, isMalicious);
                return new ExternalUrlCheckResponse("PhishTank", isMalicious);
            }
        } catch (HttpClientErrorException e) {
            log.error("Lỗi HTTP khi kiểm tra URL {} với PhishTank: status={}, body={}", url, e.getStatusCode(), e.getResponseBodyAsString());
            return new ExternalUrlCheckResponse("PhishTank", false);
        } catch (IOException e) {
            log.error("Lỗi IO khi kiểm tra URL {} với PhishTank: {}", url, e.getMessage());
            return new ExternalUrlCheckResponse("PhishTank", false);
        } catch (Exception e) {
            log.error("Lỗi không xác định khi kiểm tra URL {} với PhishTank: {}", url, e.getMessage());
            return new ExternalUrlCheckResponse("PhishTank", false);
        }
        return new ExternalUrlCheckResponse("PhishTank", false);
    }

    @Override
    public ExternalUrlCheckResponse checkUrlWithVirusTotal(String url) {
        String requestBody = String.format("url=%s", URLEncoder.encode(url, StandardCharsets.UTF_8));
        String apiUrl = virusTotalApiUrl;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("x-apikey", virusTotalApiKey);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            log.info("Gửi yêu cầu VirusTotal cho URL: {}, body: {}", url, requestBody);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                int maliciousCount = jsonNode.path("data").path("attributes").path("last_analysis_stats").path("malicious").asInt(0);
                boolean isMalicious = maliciousCount > 0;
                log.info("VirusTotal kiểm tra URL {}: maliciousCount={}", url, maliciousCount);
                return new ExternalUrlCheckResponse("VirusTotal", isMalicious);
            }
        } catch (HttpClientErrorException e) {
            log.error("Lỗi HTTP khi kiểm tra URL {} với VirusTotal: status={}, body={}", url, e.getStatusCode(), e.getResponseBodyAsString());
            return new ExternalUrlCheckResponse("VirusTotal", false);
        } catch (IOException e) {
            log.error("Lỗi IO khi kiểm tra URL {} với VirusTotal: {}", url, e.getMessage());
            return new ExternalUrlCheckResponse("VirusTotal", false);
        } catch (Exception e) {
            log.error("Lỗi không xác định khi kiểm tra URL {} với VirusTotal: {}", url, e.getMessage());
            return new ExternalUrlCheckResponse("VirusTotal", false);
        }
        return new ExternalUrlCheckResponse("VirusTotal", false);
    }

    @Override
    public ExternalUrlCheckResponse checkUrlWithUrlScan(String url) {
        try {
            // Thay thế Gson JsonObject bằng Jackson ObjectMapper
            ObjectNode jsonNode = objectMapper.createObjectNode(); // <-- SỬA Ở ĐÂY
            jsonNode.put("url", url);
            jsonNode.put("public", "on");
            String requestBody = objectMapper.writeValueAsString(jsonNode); // <-- SỬA Ở ĐÂY
            String apiUrl = urlscanApiUrl;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("API-Key", urlscanApiKey);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            log.info("Gửi yêu cầu UrlScan cho URL: {}, body: {}", url, requestBody);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseJsonNode = objectMapper.readTree(response.getBody()); // Đổi tên biến để tránh trùng
                boolean isMalicious = responseJsonNode.path("verdicts").path("overall").path("score").asDouble(0) > 50;
                log.info("UrlScan kiểm tra URL {}: isMalicious={}", url, isMalicious);
                return new ExternalUrlCheckResponse("UrlScan", isMalicious);
            }
        } catch (HttpClientErrorException e) {
            log.error("Lỗi HTTP khi kiểm tra URL {} với UrlScan: status={}, body={}", url, e.getStatusCode(), e.getResponseBodyAsString());
            return new ExternalUrlCheckResponse("UrlScan", false);
        } catch (IOException e) { // Giữ IOException để bắt lỗi writeValueAsString hoặc readTree
            log.error("Lỗi IO khi kiểm tra URL {} với UrlScan: {}", url, e.getMessage());
            return new ExternalUrlCheckResponse("UrlScan", false);
        } catch (Exception e) {
            log.error("Lỗi không xác định khi kiểm tra URL {} với UrlScan: {}", url, e.getMessage());
            return new ExternalUrlCheckResponse("UrlScan", false);
        }
        return new ExternalUrlCheckResponse("UrlScan", false);
    }

    private String createSafeBrowseRequestBody(String url) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            ObjectNode client = objectMapper.createObjectNode();
            client.put("clientId", "checkscamv2");
            client.put("clientVersion", "1.0.0");
            payload.set("client", client);

            ObjectNode threatInfo = objectMapper.createObjectNode();
            ArrayNode threatTypes = objectMapper.createArrayNode();
            threatTypes.add("MALWARE");
            threatTypes.add("SOCIAL_ENGINEERING");
            threatInfo.set("threatTypes", threatTypes);

            ArrayNode platformTypes = objectMapper.createArrayNode();
            platformTypes.add("ANY_PLATFORM");
            threatInfo.set("platformTypes", platformTypes);

            ArrayNode threatEntryTypes = objectMapper.createArrayNode();
            threatEntryTypes.add("URL");
            threatInfo.set("threatEntryTypes", threatEntryTypes);

            ArrayNode threatEntries = objectMapper.createArrayNode();
            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("url", url);
            threatEntries.add(entry);
            threatInfo.set("threatEntries", threatEntries);

            payload.set("threatInfo", threatInfo);
            return objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            log.error("Lỗi khi tạo Safe Browse request body cho URL {}: {}", url, e.getMessage());
            return "{}";
        }
    }
}