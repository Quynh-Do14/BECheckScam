package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.constant.Constant;
import com.example.checkscamv2.constant.ScamInfoType;
import com.example.checkscamv2.dto.request.CheckScamRequest;
import com.example.checkscamv2.dto.response.ExternalUrlCheckResponse;
import com.example.checkscamv2.dto.response.ScamAnalysisResponse;
import com.example.checkscamv2.entity.*;
import com.example.checkscamv2.repository.*;
import com.example.checkscamv2.service.CheckScamGeminiService;
import com.example.checkscamv2.service.ExternalUrlCheckService;
import com.example.checkscamv2.service.GeminiService; // Thay OpenRouterService
import com.example.checkscamv2.service.PlaywrightService;
import com.example.checkscamv2.util.DataUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Base64.getEncoder;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckScamGeminiServiceImpl implements CheckScamGeminiService {
    private final GeminiService geminiService; // Thay openRouterService
    private final ReportDetailRepository reportDetailRepository;
    private final AttachmentRepository attachmentRepository;
    private final ObjectMapper objectMapper;
    private final PhoneScamRepository phoneScamRepository;
    private final BankScamRepository bankScamRepository;
    private final UrlScamRepository urlScamRepository;
    private final PlaywrightService playwrightService;
    private final ExternalUrlCheckService externalUrlCheckService;

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");

    @Override
    public ScamAnalysisResponse checkScam(CheckScamRequest request) throws JsonProcessingException {
        if (request == null || request.getInfo() == null || request.getInfo().isBlank() || request.getType() == null) {
            throw new IllegalArgumentException("Thông tin đầu vào không hợp lệ: info và type là bắt buộc.");
        }

        String normalizedInfo = null;
        Integer typeInt = request.getType().getType();
        Map<String, Object> stats = new HashMap<>();
        String screenShot = null;
        List<ExternalUrlCheckResponse> externalChecks = new ArrayList<>();

        switch (request.getType()) {
            case SDT -> {
                normalizedInfo = DataUtil.normalizePhoneNumber(request.getInfo());
                DataUtil.validatePhoneNumber(normalizedInfo);
                Optional<PhoneScam> phoneScamOpt = phoneScamRepository.findByPhoneNumber(normalizedInfo);
                phoneScamOpt.ifPresent(phoneScam -> {
                    Optional.ofNullable(phoneScam.getStats()).ifPresent(s -> {
                        stats.put("phoneNumber", phoneScam.getPhoneNumber());
                        stats.put("verifiedCount", s.getVerifiedCount());
                        stats.put("lastReportAt", s.getLastReportAt());
                    });
                });
            }
            case STK -> {
                normalizedInfo = request.getInfo();
                DataUtil.validateBankAccount(normalizedInfo);
                Optional<BankScam> bankScamOpt = bankScamRepository.findByBankAccount(normalizedInfo);
                bankScamOpt.ifPresent(bankScam -> {
                    Optional.ofNullable(bankScam.getStats()).ifPresent(s -> {
                        stats.put("bankAccountNumber", bankScam.getBankAccount());
                        stats.put("verifiedCount", s.getVerifiedCount());
                        stats.put("lastReportAt", s.getLastReportAt());
                    });
                });
            }
            case URL -> {
                normalizedInfo = DataUtil.extractFullDomain(request.getInfo());
                Optional<UrlScam> urlScamOpt = urlScamRepository.findByUrl(normalizedInfo);

                if (urlScamOpt.isPresent()) {
                    UrlScam urlScam = urlScamOpt.get();
                    Optional.ofNullable(urlScam.getStats()).ifPresent(s -> {
                        stats.put("urlScam", urlScam.getUrl());
                        stats.put("verifiedCount", s.getVerifiedCount());
                        stats.put("lastReportAt", s.getLastReportAt());
                    });
                } else {
                    // Nếu không tìm thấy URL trong DB => xóa stats, chỉ phân tích ảnh
                    stats.clear();
                }

                externalChecks = externalUrlCheckService.checkUrlWithAllServices(normalizedInfo);
                screenShot = playwrightService.captureScreenshotAsUrl(normalizedInfo);
            }

            default -> throw new IllegalArgumentException("Loại thông tin không hợp lệ: " + request.getType());
        }

        // Lấy các báo cáo chi tiết có status = 2
        List<ReportDetail> reportDetails = reportDetailRepository.findByInfoAndTypeAndStatus(normalizedInfo, typeInt, 2);
        String description = null;
        List<String> evidenceUrls = Collections.emptyList();
        String aiAnalysisResult = null;
        String moneyScam = null;
        LocalDateTime dateReport = null;

        // Đối với SDT và STK, chỉ tra thông tin nếu có báo cáo với status = 2
        if ((request.getType() == ScamInfoType.SDT || request.getType() == ScamInfoType.STK) && reportDetails.isEmpty()) {
            String noReportMsg = "Chưa có báo cáo";
            return ScamAnalysisResponse.builder()
                    .info(normalizedInfo)
                    .type(request.getType().ordinal() + 1)
                    .description(noReportMsg)
                    .reportDescription(noReportMsg)
                    .moneyScam(null)
                    .dateReport(null)
                    .verifiedCount(0)
                    .lastReportAt(null)
                    .evidenceUrls(null)
                    .screenShot(null)
                    .analysis(noReportMsg)
                    .externalUrlCheckResponses(null)
                    .build();
        }

        if (reportDetails.isEmpty()) {
            if (request.getType() == ScamInfoType.URL) {
                description = "Không có mô tả chi tiết";
                dateReport = stats.containsKey("lastReportAt") && stats.get("lastReportAt") instanceof LocalDateTime ?
                        (LocalDateTime) stats.get("lastReportAt") : LocalDateTime.now();
                aiAnalysisResult = formatAnalysis("Không có dữ liệu chi tiết. Vui lòng kiểm tra ảnh chụp và số lần xác thực (" +
                        (stats.getOrDefault("verifiedCount", 0) instanceof Integer ? stats.get("verifiedCount") : 0) + ") để phân tích thêm.");
            }
        } else {
            ReportDetail firstReportDetail = reportDetails.get(0);
            description = firstReportDetail.getDescription();
            Object moneyScamValue = firstReportDetail.getReport().getMoneyScam();
            if (moneyScamValue != null) {
                try {
                    long moneyScamLong = parseMoneyScam(moneyScamValue);
                    moneyScam = MONEY_FORMAT.format(moneyScamLong) + " VND";
                } catch (NumberFormatException e) {
                    log.warn("Invalid moneyScam format: {}", moneyScamValue);
                    moneyScam = String.valueOf(moneyScamValue);
                }
            }
            dateReport = firstReportDetail.getReport().getDateReport();

            List<Long> reportIds = reportDetails.stream()
                    .map(ReportDetail::getReport)
                    .map(Report::getId)
                    .collect(Collectors.toList());

            List<Attachment> attachments = attachmentRepository.findByReportIdIn(reportIds);
            if (request.getType() != ScamInfoType.URL) {
                evidenceUrls = attachments.stream()
                        .map(Attachment::getUrl)
                        .collect(Collectors.toList());
            }
        }

        String prompt = "Chỉ phân tích ảnh";
        if (!stats.isEmpty()) {
            prompt = String.format(Constant.PROMPT_SCAM_ANALYSIS,
                    normalizedInfo != null ? normalizedInfo : "N/A",
                    typeInt,
                    description != null ? description : "Không có mô tả chi tiết.",
                    objectMapper.writeValueAsString(stats));
        }

        if (aiAnalysisResult == null || aiAnalysisResult.isEmpty()) {
            String textAnalysis = geminiService.analyzeScamData(prompt); // Thay openRouterService
            aiAnalysisResult = formatAnalysis(textAnalysis);
        }

        if (request.getType() == ScamInfoType.URL && screenShot != null) {
            try {
                File screenshotFile = new File(screenShot.replaceFirst("^/", ""));
                if (screenshotFile.exists()) {
                    byte[] imageBytes = Files.readAllBytes(screenshotFile.toPath());
                    String base64Image = getEncoder().encodeToString(imageBytes);
                    String imageAnalysis = geminiService.analyzeScreenShot(base64Image); // Thay openRouterService
                    if (imageAnalysis != null && !imageAnalysis.isEmpty()) {
                        aiAnalysisResult = (aiAnalysisResult != null && !aiAnalysisResult.isEmpty())
                                ? aiAnalysisResult + "\n\n---\n**Phân tích ảnh:**\n" + imageAnalysis
                                : "**Phân tích ảnh:**\n" + imageAnalysis;
                    }
                }
            } catch (IOException e) {
                log.error("Failed to read screenshot file {} for analysis: {}", screenShot, e.getMessage(), e);
            }
        }

        return ScamAnalysisResponse.builder()
                .info(normalizedInfo)
                .type(request.getType().ordinal() + 1)
                .description(description != null ? description : "Không có mô tả chi tiết.")
                .reportDescription(reportDetails.isEmpty() ? "Không có thông tin báo cáo chi tiết." : "Đã tìm thấy thông tin báo cáo liên quan.")
                .moneyScam(moneyScam)
                .dateReport(dateReport)
                .verifiedCount((Integer) stats.getOrDefault("verifiedCount", 0))
                .lastReportAt(stats.containsKey("lastReportAt") &&
                        stats.get("lastReportAt") instanceof LocalDateTime &&
                        !stats.get("lastReportAt").toString().startsWith("0001") ?
                        (LocalDateTime) stats.get("lastReportAt") : null)
                .evidenceUrls(evidenceUrls.isEmpty() ? null : evidenceUrls)
                .screenShot(screenShot)
                .analysis(aiAnalysisResult)
                .externalUrlCheckResponses(externalChecks)
                .build();
    }

    @Override
    public String analyzeScamData(String data) {
        if (data == null || data.isBlank()) {
            throw new IllegalArgumentException("Dữ liệu phân tích không được để trống.");
        }
        return formatAnalysis(geminiService.analyzeScamData(data)); // Thay openRouterService
    }

    private String formatAnalysis(String fullAnalysis) {
        if (fullAnalysis == null || fullAnalysis.isEmpty()) {
            return "Không có phân tích chi tiết.";
        }

        StringBuilder formatted = new StringBuilder();
        String[] lines = fullAnalysis.split("\n");
        boolean inSection = false;
        String currentSection = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Xác định tiêu đề mục lớn
            if (line.matches("\\*\\*\\d+\\.\\s+.*\\*\\*")) {
                if (currentSection != null) {
                    formatted.append("\n");
                }
                currentSection = line.replaceAll("\\*\\*", "").trim();
                formatted.append(currentSection).append("\n");
                inSection = true;
                continue;
            }

            // Xử lý các ý nhỏ
            if (inSection) {
                if (line.startsWith("*") || line.startsWith("-")) {
                    String cleanedLine = line.replaceFirst("^[*\\-]\\s*", "- ");
                    formatted.append(cleanedLine).append("\n");
                } else if (line.matches("\\s*\\w+\\..*")) {
                    formatted.append("- ").append(line.trim()).append("\n");
                } else {
                    formatted.append("  ").append(line).append("\n");
                }
            }
        }

        return formatted.toString().trim();
    }

    private long parseMoneyScam(Object moneyScamValue) {
        if (moneyScamValue instanceof Number number) {
            return number.longValue();
        } else if (moneyScamValue instanceof String stringValue) {
            String cleanedValue = stringValue.replaceAll("[^0-9]", "");
            return Long.parseLong(cleanedValue);
        }
        throw new NumberFormatException("Cannot parse moneyScam value: " + moneyScamValue);
    }

    @Override
    public String analyzeScreenShot(String base64Image) {
        if (base64Image == null || base64Image.isBlank()) {
            throw new IllegalArgumentException("Ảnh không được để trống.");
        }
        String result = geminiService.analyzeScreenShot(base64Image); // Thay openRouterService
        return result;
    }

    @Override
    public String chatWithAI(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Câu hỏi không được để trống.");
        }
        String prompt = Constant.PROMPT_CHAT_WITH_AI + message;
        String response = geminiService.chatbot(prompt); // Thay openRouterService.chatbot
        return response == null || response.trim().isEmpty() ? "Xin lỗi, không thể cung cấp phản hồi cho câu hỏi của bạn." : response;
    }
}