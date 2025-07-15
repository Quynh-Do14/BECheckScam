package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.dto.response.SubjectDetailResponseDTO;
import com.example.checkscamv2.entity.*;
import com.example.checkscamv2.repository.*;
import com.example.checkscamv2.service.ScamStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.example.checkscamv2.dto.response.TopScamItemResponseDTO;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScamStatsServiceImpl implements ScamStatsService {

    // ===== DEPENDENCIES =====
    private final UrlScamStatsRepository urlRepository;
    private final PhoneScamStatsRepository phoneRepository;
    private final BankScamStatsRepository bankRepository;
    private final ScamTypeRepository scamTypeRepository;
    private final ReportDetailRepository reportDetailRepository;
    private final ReportRepository reportRepository;
    private final AttachmentRepository attachmentRepository;
    private final PhoneScamRepository phoneScamRepository;
    private final BankScamRepository bankScamRepository;
    private final UrlScamRepository urlScamRepository;

    // ===== CONSTANTS =====
    private static final String BASE_URL = "";
    private static final String DEFAULT_REPORTER_LOCATION = "Việt Nam";
    private static final Pattern MONEY_PATTERN = Pattern.compile("[^0-9]");
    private static final int RELATED_SUBJECTS_LIMIT = 3;

    // Default names
    private static final String DEFAULT_PHONE_NAME = "Số điện thoại nghi vấn";
    private static final String DEFAULT_BANK_NAME = "Tài khoản ngân hàng nghi vấn";
    private static final String DEFAULT_URL_NAME = "Website nghi vấn";
    private static final String DEFAULT_UNKNOWN_NAME = "Đối tượng không xác định";

    // Status messages
    private static final Map<Integer, String> STATUS_MESSAGES = Map.of(
        1, "Đã xác minh",
        2, "Đã từ chối"
    );
    private static final String DEFAULT_STATUS = "Đang xử lý";

    // Risk levels
    private static final String RISK_LOW = "low";
    private static final String RISK_MEDIUM = "medium";
    private static final String RISK_HIGH = "high";

    // Fallback descriptions
    private static final Map<Integer, String> FALLBACK_DESCRIPTIONS = Map.of(
        1, "Đây là số điện thoại được ghi nhận trong hệ thống nhưng chưa có báo cáo chi tiết từ người dùng.",
        2, "Đây là tài khoản ngân hàng được ghi nhận trong hệ thống nhưng chưa có báo cáo chi tiết từ người dùng.",
        3, "Đây là website được ghi nhận trong hệ thống nhưng chưa có báo cáo chi tiết từ người dùng."
    );

    private static final String DEFAULT_DESCRIPTION = "Thông tin chi tiết về đối tượng này đang được cập nhật.";
    private static final String DEFAULT_REPORT_DESCRIPTION = "Báo cáo lừa đảo";

    // ===== TOP SCAMS API =====

    @Override
    public List<TopScamItemResponseDTO> getTopPhoneScams() {
        log.debug("Getting top phone scams");
        try {
            return phoneRepository.getTopPhonesByViews();
        } catch (Exception e) {
            log.error("Error getting top phone scams: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<TopScamItemResponseDTO> getTopBankScams() {
        log.debug("Getting top bank scams");
        try {
            return bankRepository.getTopBanksByViews();
        } catch (Exception e) {
            log.error("Error getting top bank scams: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<TopScamItemResponseDTO> getTopUrlScams() {
        log.debug("Getting top URL scams");
        try {
            return urlRepository.getTopUrlsByViews();
        } catch (Exception e) {
            log.error("Error getting top URL scams: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // ===== VIEW COUNT INCREMENT API =====

    @Override
    @Transactional
    public void incrementPhoneViewCount(String phoneNumber) {
        log.info("Incrementing view count for phone: {}", phoneNumber);

        try {
            phoneRepository.findByPhoneNumber(phoneNumber)
                .ifPresentOrElse(
                    this::incrementExistingPhoneStats,
                    () -> createNewPhoneStats(phoneNumber)
                );
        } catch (Exception e) {
            log.error("Error incrementing phone view count for {}: {}", phoneNumber, e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void incrementBankViewCount(String bankAccount) {
        log.info("Incrementing view count for bank account: {}", bankAccount);

        try {
            bankRepository.findByBankAccount(bankAccount)
                .ifPresentOrElse(
                    this::incrementExistingBankStats,
                    () -> log.warn("Bank account not found in stats: {}", bankAccount)
                );
        } catch (Exception e) {
            log.error("Error incrementing bank view count for {}: {}", bankAccount, e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void incrementUrlViewCount(String url) {
        log.info("Incrementing view count for URL: {}", url);

        try {
            urlRepository.findByUrl(url)
                .ifPresentOrElse(
                    this::incrementExistingUrlStats,
                    () -> log.warn("URL not found in stats: {}", url)
                );
        } catch (Exception e) {
            log.error("Error incrementing URL view count for {}: {}", url, e.getMessage(), e);
        }
    }

    // ===== SUBJECT DETAIL API =====

    @Override
    public SubjectDetailResponseDTO getSubjectDetail(String info, Integer type) {
        log.info("Getting subject detail for info={}, type={}", info, type);

        try {
            validateInput(info, type);

            return buildDetailedResponse(info, type)
                .or(() -> buildFallbackResponse(info, type))
                .orElseGet(() -> {
                    log.warn("No data found for subject: {} type: {}", info, type);
                    return null;
                });

        } catch (Exception e) {
            log.error("Error getting subject detail for info={}, type={}: {}", info, type, e.getMessage(), e);
            return null;
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    private void validateInput(String info, Integer type) {
        if (!StringUtils.hasText(info)) {
            throw new IllegalArgumentException("Subject info cannot be empty");
        }
        if (type == null || type < 1 || type > 3) {
            throw new IllegalArgumentException("Invalid subject type: " + type);
        }
        if (info.length() > 500) {
            throw new IllegalArgumentException("Subject info too long: " + info.length());
        }
    }

    // ===== DETAILED RESPONSE BUILDING =====

    private Optional<SubjectDetailResponseDTO> buildDetailedResponse(String info, Integer type) {
        List<ReportDetail> reportDetails = reportDetailRepository.findByInfoAndType(info, type);

        if (reportDetails.isEmpty()) {
            return Optional.empty();
        }

        // Aggregate data efficiently
        SubjectDataBundle dataBundle = aggregateSubjectData(info, type, reportDetails);

        // Build comprehensive response
        SubjectDetailResponseDTO response = createDetailedResponse(dataBundle);
        return Optional.of(response);
    }

    private SubjectDataBundle aggregateSubjectData(String info, Integer type, List<ReportDetail> reportDetails) {
        // Extract report IDs efficiently using Set to avoid duplicates
        Set<Long> reportIds = reportDetails.stream()
                .map(rd -> rd.getReport().getId())
                .collect(Collectors.toSet());

        // Convert Set to List for repository methods
        List<Long> reportIdsList = new ArrayList<>(reportIds);

        // Batch fetch related data
        List<Report> reports = reportRepository.findAllById(reportIdsList);
        List<Attachment> attachments = attachmentRepository.findByReportIdIn(reportIdsList);

        return new SubjectDataBundle(info, type, reportDetails, reports, attachments);
    }

    private SubjectDetailResponseDTO createDetailedResponse(SubjectDataBundle data) {
        return SubjectDetailResponseDTO.builder()
                .info(data.getInfo())
                .type(data.getType())
                .name(resolveSubjectName(data.getInfo(), data.getType()))
                .description(extractBestDescription(data.getReportDetails()))
                .totalScamAmount(calculateTotalAmount(data.getReports()))
                .reportCount(data.getReports().size())
                .lastReportDate(findLatestDate(data.getReports()))
                .riskLevel(assessRiskLevel(data.getReports().size()))
                .evidenceImages(processEvidenceUrls(data.getAttachments()))
                .reports(mapToReportItems(data.getReports()))
                .relatedSubjects(findRelatedSubjects(data.getInfo(), data.getType()))
                .build();
    }

    // ===== FALLBACK RESPONSE BUILDING =====

    private Optional<SubjectDetailResponseDTO> buildFallbackResponse(String info, Integer type) {
        return switch (type) {
            case 1 -> buildPhoneFallback(info);
            case 2 -> buildBankFallback(info);
            case 3 -> buildUrlFallback(info);
            default -> Optional.empty();
        };
    }

    private Optional<SubjectDetailResponseDTO> buildPhoneFallback(String phoneNumber) {
        return phoneScamRepository.findByPhoneNumber(phoneNumber)
                .map(phoneScam -> createBasicResponse(
                    phoneNumber, 1,
                    resolvePhoneName(phoneScam),
                    getPhoneStatsOrDefault(phoneNumber).getVerifiedCount()
                ));
    }

    private Optional<SubjectDetailResponseDTO> buildBankFallback(String bankAccount) {
        return bankScamRepository.findByBankAccount(bankAccount)
                .map(bankScam -> createBasicResponse(
                    bankAccount, 2,
                    resolveBankName(bankScam),
                    getBankStatsOrDefault(bankAccount).getVerifiedCount()
                ));
    }

    private Optional<SubjectDetailResponseDTO> buildUrlFallback(String url) {
        return urlScamRepository.findByUrl(url)
                .map(urlScam -> createBasicResponse(
                    url, 3,
                    DEFAULT_URL_NAME,
                    getUrlStatsOrDefault(url).getVerifiedCount()
                ));
    }

    private SubjectDetailResponseDTO createBasicResponse(String info, Integer type, String name, Integer reportCount) {
        return SubjectDetailResponseDTO.builder()
                .info(info)
                .type(type)
                .name(name)
                .description(FALLBACK_DESCRIPTIONS.get(type))
                .totalScamAmount(0L)
                .reportCount(Optional.ofNullable(reportCount).orElse(0))
                .lastReportDate(null)
                .riskLevel(assessRiskLevel(reportCount))
                .evidenceImages(Collections.emptyList())
                .reports(Collections.emptyList())
                .relatedSubjects(findRelatedSubjects(info, type))
                .build();
    }

    // ===== UTILITY METHODS =====

    private String resolveSubjectName(String info, Integer type) {
        return switch (type) {
            case 1 -> phoneScamRepository.findByPhoneNumber(info)
                    .map(this::resolvePhoneName)
                    .orElse(DEFAULT_PHONE_NAME);
            case 2 -> bankScamRepository.findByBankAccount(info)
                    .map(this::resolveBankName)
                    .orElse(DEFAULT_BANK_NAME);
            case 3 -> DEFAULT_URL_NAME;
            default -> DEFAULT_UNKNOWN_NAME;
        };
    }

    private String resolvePhoneName(PhoneScam phoneScam) {
        return StringUtils.hasText(phoneScam.getOwnerName())
                ? phoneScam.getOwnerName()
                : DEFAULT_PHONE_NAME;
    }

    private String resolveBankName(BankScam bankScam) {
        StringBuilder name = new StringBuilder();

        if (StringUtils.hasText(bankScam.getBankName())) {
            name.append(bankScam.getBankName());
        }

        if (StringUtils.hasText(bankScam.getNameAccount())) {
            if (name.length() > 0) name.append(" - ");
            name.append(bankScam.getNameAccount());
        }

        return name.length() > 0 ? name.toString() : DEFAULT_BANK_NAME;
    }

    private String extractBestDescription(List<ReportDetail> reportDetails) {
        return reportDetails.stream()
                .map(ReportDetail::getDescription)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(DEFAULT_DESCRIPTION);
    }

    private long calculateTotalAmount(List<Report> reports) {
        return reports.stream()
                .map(Report::getMoneyScam)
                .filter(StringUtils::hasText)
                .mapToLong(this::parseMoneyAmount)
                .sum();
    }

    private long parseMoneyAmount(String moneyString) {
        try {
            String numbersOnly = MONEY_PATTERN.matcher(moneyString).replaceAll("");
            return StringUtils.hasText(numbersOnly) ? Long.parseLong(numbersOnly) : 0L;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse money amount: {}", moneyString);
            return 0L;
        }
    }

    private LocalDateTime findLatestDate(List<Report> reports) {
        return reports.stream()
                .map(Report::getDateReport)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private String assessRiskLevel(Integer reportCount) {
        if (reportCount == null || reportCount == 0) return RISK_LOW;
        if (reportCount >= 10) return RISK_HIGH;
        if (reportCount >= 3) return RISK_MEDIUM;
        return RISK_LOW;
    }

    private List<String> processEvidenceUrls(List<Attachment> attachments) {
        return attachments.stream()
                .map(Attachment::getUrl)
                .filter(StringUtils::hasText)
                .map(this::normalizeUrl)
                .collect(Collectors.toList());
    }

    private String normalizeUrl(String url) {
        if (url.startsWith(BASE_URL)) {
            return url.replaceFirst("^" + Pattern.quote(BASE_URL), "");
        }
        return url.startsWith("/") ? url : "/" + url;
    }

    private List<SubjectDetailResponseDTO.ReportItemDTO> mapToReportItems(List<Report> reports) {
        return reports.stream()
                .map(this::mapToReportItem)
                .collect(Collectors.toList());
    }

    private SubjectDetailResponseDTO.ReportItemDTO mapToReportItem(Report report) {
        return SubjectDetailResponseDTO.ReportItemDTO.builder()
                .id(report.getId())
                .date(report.getDateReport())
                .description(StringUtils.hasText(report.getDescription())
                        ? report.getDescription()
                        : DEFAULT_REPORT_DESCRIPTION)
                .amount(parseMoneyAmount(report.getMoneyScam()))
                .reporterLocation(DEFAULT_REPORTER_LOCATION)
                .status(STATUS_MESSAGES.getOrDefault(report.getStatus(), DEFAULT_STATUS))
                .build();
    }

    private List<SubjectDetailResponseDTO.RelatedSubjectDTO> findRelatedSubjects(String info, Integer type) {
        try {
            List<TopScamItemResponseDTO> topItems = getTopItemsByType(type);

            return topItems.stream()
                    .filter(item -> !Objects.equals(item.getInfo(), info))
                    .limit(RELATED_SUBJECTS_LIMIT)
                    .map(item -> mapToRelatedSubject(item, type))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting related subjects for info: {}, type: {}", info, type, e);
            return Collections.emptyList();
        }
    }

    private List<TopScamItemResponseDTO> getTopItemsByType(Integer type) {
        return switch (type) {
            case 1 -> getTopPhoneScams();
            case 2 -> getTopBankScams();
            case 3 -> getTopUrlScams();
            default -> Collections.emptyList();
        };
    }

    private SubjectDetailResponseDTO.RelatedSubjectDTO mapToRelatedSubject(TopScamItemResponseDTO item, Integer type) {
        return SubjectDetailResponseDTO.RelatedSubjectDTO.builder()
                .info(item.getInfo())
                .type(type)
                .description(item.getDescription())
                .riskLevel(item.getStatus())
                .build();
    }

    // ===== VIEW COUNT INCREMENT HELPERS =====

    private void incrementExistingPhoneStats(PhoneScamStats stats) {
        stats.setViewCount(stats.getViewCount() + 1);
        phoneRepository.save(stats);
        log.debug("Incremented existing phone stats for: {}",
                stats.getPhoneScam() != null ? stats.getPhoneScam().getPhoneNumber() : "unknown");
    }

    private void incrementExistingBankStats(BankScamStats stats) {
        stats.setViewCount(stats.getViewCount() + 1);
        bankRepository.save(stats);
        log.debug("Incremented existing bank stats for: {}",
                stats.getBankScam() != null ? stats.getBankScam().getBankAccount() : "unknown");
    }

    private void incrementExistingUrlStats(UrlScamStats stats) {
        stats.setViewCount(stats.getViewCount() + 1);
        urlRepository.save(stats);
        log.debug("Incremented existing URL stats for: {}",
                stats.getUrlScam() != null ? stats.getUrlScam().getUrl() : "unknown");
    }

    private void createNewPhoneStats(String phoneNumber) {
        phoneScamRepository.findByPhoneNumber(phoneNumber)
                .ifPresentOrElse(
                    phoneScam -> {
                        PhoneScamStats newStats = PhoneScamStats.builder()
                                .id(phoneScam.getId())
                                .phoneScam(phoneScam)
                                .verifiedCount(0)
                                .viewCount(1)
                                .build();
                        phoneRepository.save(newStats);
                        log.debug("Created new phone stats for: {}", phoneNumber);
                    },
                    () -> log.warn("Phone scam not found when creating stats: {}", phoneNumber)
                );
    }

    // ===== STATS GETTER HELPERS =====

    private PhoneScamStats getPhoneStatsOrDefault(String phoneNumber) {
        return phoneRepository.findByPhoneNumber(phoneNumber)
                .orElse(PhoneScamStats.builder()
                        .viewCount(0)
                        .verifiedCount(0)
                        .build());
    }

    private BankScamStats getBankStatsOrDefault(String bankAccount) {
        return bankRepository.findByBankAccount(bankAccount)
                .orElse(BankScamStats.builder()
                        .viewCount(0)
                        .verifiedCount(0)
                        .build());
    }

    private UrlScamStats getUrlStatsOrDefault(String url) {
        return urlRepository.findByUrl(url)
                .orElse(UrlScamStats.builder()
                        .viewCount(0)
                        .verifiedCount(0)
                        .build());
    }

    // ===== LEGACY METHODS (for backward compatibility) =====

    /**
     * @deprecated Use resolveSubjectName instead
     */
    @Deprecated
    private String getSubjectName(String info, Integer type) {
        return resolveSubjectName(info, type);
    }

    /**
     * @deprecated Use extractBestDescription instead
     */
    @Deprecated
    private String getSubjectDescription(List<ReportDetail> reportDetails) {
        return extractBestDescription(reportDetails);
    }

    /**
     * @deprecated Use mapToReportItem instead
     */
    @Deprecated
    private SubjectDetailResponseDTO.ReportItemDTO buildReportItem(Report report) {
        return mapToReportItem(report);
    }

    /**
     * @deprecated Use STATUS_MESSAGES.getOrDefault instead
     */
    @Deprecated
    private String getReportStatus(Integer status) {
        return STATUS_MESSAGES.getOrDefault(status, DEFAULT_STATUS);
    }

    /**
     * @deprecated Use findRelatedSubjects instead
     */
    @Deprecated
    private List<SubjectDetailResponseDTO.RelatedSubjectDTO> getRelatedSubjects(String info, Integer type) {
        return findRelatedSubjects(info, type);
    }

    /**
     * @deprecated Use assessRiskLevel instead
     */
    @Deprecated
    private String determineRiskLevel(Integer reportCount) {
        return assessRiskLevel(reportCount);
    }

    /**
     * @deprecated Use buildFallbackResponse instead
     */
    @Deprecated
    private SubjectDetailResponseDTO createFallbackResponse(String info, Integer type) {
        return buildFallbackResponse(info, type).orElse(null);
    }

    /**
     * @deprecated Use buildPhoneFallback instead
     */
    @Deprecated
    private SubjectDetailResponseDTO createPhoneFallbackResponse(String phoneNumber) {
        return buildPhoneFallback(phoneNumber).orElse(null);
    }

    /**
     * @deprecated Use buildBankFallback instead
     */
    @Deprecated
    private SubjectDetailResponseDTO createBankFallbackResponse(String bankAccount) {
        return buildBankFallback(bankAccount).orElse(null);
    }

    /**
     * @deprecated Use buildUrlFallback instead
     */
    @Deprecated
    private SubjectDetailResponseDTO createUrlFallbackResponse(String url) {
        return buildUrlFallback(url).orElse(null);
    }

    // ===== INNER DATA CLASS =====

    /**
     * Internal data container for subject-related information
     * Immutable and lightweight for better performance
     */
    private static class SubjectDataBundle {
        private final String info;
        private final Integer type;
        private final List<ReportDetail> reportDetails;
        private final List<Report> reports;
        private final List<Attachment> attachments;

        public SubjectDataBundle(String info, Integer type, List<ReportDetail> reportDetails,
                                List<Report> reports, List<Attachment> attachments) {
            this.info = info;
            this.type = type;
            this.reportDetails = List.copyOf(reportDetails);  // Immutable copy
            this.reports = List.copyOf(reports);              // Immutable copy
            this.attachments = List.copyOf(attachments);      // Immutable copy
        }

        public String getInfo() { return info; }
        public Integer getType() { return type; }
        public List<ReportDetail> getReportDetails() { return reportDetails; }
        public List<Report> getReports() { return reports; }
        public List<Attachment> getAttachments() { return attachments; }
    }
}
