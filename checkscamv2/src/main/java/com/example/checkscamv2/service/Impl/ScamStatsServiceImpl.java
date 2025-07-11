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

    // ============================= CẤU HÌNH VÀ DEPENDENCIES =============================

    // Repository dependencies
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

    // Các hằng số cấu hình

    private static final String DEFAULT_REPORTER_LOCATION = "Việt Nam";
    private static final Pattern MONEY_PATTERN = Pattern.compile("[^0-9]");
    private static final int RELATED_SUBJECTS_LIMIT = 3;

    // Tên mặc định cho các loại đối tượng
    private static final String DEFAULT_PHONE_NAME = "Số điện thoại nghi vấn";
    private static final String DEFAULT_BANK_NAME = "Tài khoản ngân hàng nghi vấn";
    private static final String DEFAULT_URL_NAME = "Website nghi vấn";
    private static final String DEFAULT_UNKNOWN_NAME = "Đối tượng không xác định";

    // Trạng thái và thông báo
    private static final Map<Integer, String> STATUS_MESSAGES = Map.of(
            1, "Đã xác minh",
            2, "Đã từ chối"
    );
    private static final String DEFAULT_STATUS = "Đang xử lý";

    // Mức độ rủi ro
    private static final String RISK_LOW = "low";
    private static final String RISK_MEDIUM = "medium";
    private static final String RISK_HIGH = "high";

    // Mô tả dự phòng cho từng loại
    private static final Map<Integer, String> FALLBACK_DESCRIPTIONS = Map.of(
            1, "Đây là số điện thoại được ghi nhận trong hệ thống nhưng chưa có báo cáo chi tiết từ người dùng.",
            2, "Đây là tài khoản ngân hàng được ghi nhận trong hệ thống nhưng chưa có báo cáo chi tiết từ người dùng.",
            3, "Đây là website được ghi nhận trong hệ thống nhưng chưa có báo cáo chi tiết từ người dùng."
    );

    private static final String DEFAULT_DESCRIPTION = "Thông tin chi tiết về đối tượng này đang được cập nhật.";
    private static final String DEFAULT_REPORT_DESCRIPTION = "Báo cáo lừa đảo";


    // ============================= API CHÍNH - LẤY DANH SÁCH TOP =============================

    /**
     * Lấy danh sách top số điện thoại lừa đảo (theo lượt xem)
     */
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

    /**
     * Lấy danh sách top tài khoản ngân hàng lừa đảo (theo lượt xem)
     */
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

    /**
     * Lấy danh sách top website lừa đảo (theo lượt xem)
     */
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


    // ============================= API CHÍNH - TĂNG LƯỢT XEM =============================

    /**
     * Tăng lượt xem cho số điện thoại
     */
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

    /**
     * Tăng lượt xem cho tài khoản ngân hàng
     */
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

    /**
     * Tăng lượt xem cho URL
     */
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


    // ============================= API CHÍNH - CHI TIẾT ĐỐI TƯỢNG =============================

    /**
     * Lấy thông tin chi tiết của một đối tượng (phone/bank/url)
     * @param info - thông tin đối tượng (số phone, tài khoản bank, url)
     * @param type - loại đối tượng (1=phone, 2=bank, 3=url)
     */
    @Override
    public SubjectDetailResponseDTO getSubjectDetail(String info, Integer type) {
        log.info("Getting subject detail for info={}, type={}", info, type);

        try {
            // Bước 1: Kiểm tra dữ liệu đầu vào
            validateInput(info, type);

            // Bước 2: Thử build response chi tiết (có báo cáo)
            return buildDetailedResponse(info, type)
                    // Bước 3: Nếu không có, dùng response dự phòng (chỉ có thông tin cơ bản)
                    .or(() -> buildFallbackResponse(info, type))
                    // Bước 4: Nếu vẫn không có gì, trả về null
                    .orElseGet(() -> {
                        log.warn("No data found for subject: {} type: {}", info, type);
                        return null;
                    });

        } catch (Exception e) {
            log.error("Error getting subject detail for info={}, type={}: {}", info, type, e.getMessage(), e);
            return null;
        }
    }


    // ============================= XỬ LÝ TĂNG LƯỢT XEM =============================

    /**
     * Tăng lượt xem cho thống kê phone đã tồn tại
     */
    private void incrementExistingPhoneStats(PhoneScamStats stats) {
        stats.setViewCount(stats.getViewCount() + 1);
        phoneRepository.save(stats);
        log.debug("Incremented existing phone stats for: {}",
                stats.getPhoneScam() != null ? stats.getPhoneScam().getPhoneNumber() : "unknown");
    }

    /**
     * Tăng lượt xem cho thống kê bank đã tồn tại
     */
    private void incrementExistingBankStats(BankScamStats stats) {
        stats.setViewCount(stats.getViewCount() + 1);
        bankRepository.save(stats);
        log.debug("Incremented existing bank stats for: {}",
                stats.getBankScam() != null ? stats.getBankScam().getBankAccount() : "unknown");
    }

    /**
     * Tăng lượt xem cho thống kê URL đã tồn tại
     */
    private void incrementExistingUrlStats(UrlScamStats stats) {
        stats.setViewCount(stats.getViewCount() + 1);
        urlRepository.save(stats);
        log.debug("Incremented existing URL stats for: {}",
                stats.getUrlScam() != null ? stats.getUrlScam().getUrl() : "unknown");
    }

    /**
     * Tạo thống kê mới cho phone (nếu chưa tồn tại)
     */
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


    // ============================= XỬ LÝ RESPONSE CHI TIẾT =============================

    /**
     * Kiểm tra tính hợp lệ của dữ liệu đầu vào
     */
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

    /**
     * Build response chi tiết (khi có báo cáo từ người dùng)
     */
    private Optional<SubjectDetailResponseDTO> buildDetailedResponse(String info, Integer type) {
        // Tìm các báo cáo chi tiết cho đối tượng này
        List<ReportDetail> reportDetails = reportDetailRepository.findByInfoAndType(info, type);

        if (reportDetails.isEmpty()) {
            return Optional.empty();
        }

        // Thu thập tất cả dữ liệu liên quan
        SubjectDataBundle dataBundle = aggregateSubjectData(info, type, reportDetails);

        // Tạo response đầy đủ
        SubjectDetailResponseDTO response = createDetailedResponse(dataBundle);
        return Optional.of(response);
    }

    /**
     * Thu thập tất cả dữ liệu liên quan đến subject
     */
    private SubjectDataBundle aggregateSubjectData(String info, Integer type, List<ReportDetail> reportDetails) {
        // Lấy danh sách ID report (loại bỏ trùng lặp)
        Set<Long> reportIds = reportDetails.stream()
                .map(rd -> rd.getReport().getId())
                .collect(Collectors.toSet());

        List<Long> reportIdsList = new ArrayList<>(reportIds);

        // Lấy dữ liệu liên quan một lần (batch fetch để tối ưu performance)
        List<Report> reports = reportRepository.findAllById(reportIdsList);
        List<Attachment> attachments = attachmentRepository.findByReportIdIn(reportIdsList);

        return new SubjectDataBundle(info, type, reportDetails, reports, attachments);
    }

    /**
     * Tạo response chi tiết từ dữ liệu đã thu thập
     */
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


    // ============================= XỬ LÝ RESPONSE DỰ PHÒNG =============================

    /**
     * Build response dự phòng (khi không có báo cáo chi tiết)
     */
    private Optional<SubjectDetailResponseDTO> buildFallbackResponse(String info, Integer type) {
        return switch (type) {
            case 1 -> buildPhoneFallback(info);       // Phone
            case 2 -> buildBankFallback(info);        // Bank
            case 3 -> buildUrlFallback(info);         // URL
            default -> Optional.empty();
        };
    }

    /**
     * Response dự phòng cho số điện thoại
     */
    private Optional<SubjectDetailResponseDTO> buildPhoneFallback(String phoneNumber) {
        return phoneScamRepository.findByPhoneNumber(phoneNumber)
                .map(phoneScam -> createBasicResponse(
                        phoneNumber, 1,
                        resolvePhoneName(phoneScam),
                        getPhoneStatsOrDefault(phoneNumber).getVerifiedCount()
                ));
    }

    /**
     * Response dự phòng cho tài khoản ngân hàng
     */
    private Optional<SubjectDetailResponseDTO> buildBankFallback(String bankAccount) {
        return bankScamRepository.findByBankAccount(bankAccount)
                .map(bankScam -> createBasicResponse(
                        bankAccount, 2,
                        resolveBankName(bankScam),
                        getBankStatsOrDefault(bankAccount).getVerifiedCount()
                ));
    }

    /**
     * Response dự phòng cho URL
     */
    private Optional<SubjectDetailResponseDTO> buildUrlFallback(String url) {
        return urlScamRepository.findByUrl(url)
                .map(urlScam -> createBasicResponse(
                        url, 3,
                        DEFAULT_URL_NAME,
                        getUrlStatsOrDefault(url).getVerifiedCount()
                ));
    }

    /**
     * Tạo response cơ bản (dùng cho fallback)
     */
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


    // ============================= HÀM TIỆN ÍCH - XỬ LÝ DỮ LIỆU =============================

    /**
     * Xác định tên hiển thị cho đối tượng
     */
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

    /**
     * Xác định tên cho số điện thoại
     */
    private String resolvePhoneName(PhoneScam phoneScam) {
        return StringUtils.hasText(phoneScam.getOwnerName())
                ? phoneScam.getOwnerName()
                : DEFAULT_PHONE_NAME;
    }

    /**
     * Xác định tên cho tài khoản ngân hàng
     */
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

    /**
     * Lấy mô tả tốt nhất từ danh sách báo cáo
     */
    private String extractBestDescription(List<ReportDetail> reportDetails) {
        return reportDetails.stream()
                .map(ReportDetail::getDescription)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(DEFAULT_DESCRIPTION);
    }

    /**
     * Tính tổng số tiền lừa đảo
     */
    private long calculateTotalAmount(List<Report> reports) {
        return reports.stream()
                .map(Report::getMoneyScam)
                .filter(StringUtils::hasText)
                .mapToLong(this::parseMoneyAmount)
                .sum();
    }

    /**
     * Chuyển đổi chuỗi tiền thành số
     */
    private long parseMoneyAmount(String moneyString) {
        try {
            String numbersOnly = MONEY_PATTERN.matcher(moneyString).replaceAll("");
            return StringUtils.hasText(numbersOnly) ? Long.parseLong(numbersOnly) : 0L;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse money amount: {}", moneyString);
            return 0L;
        }
    }

    /**
     * Tìm ngày báo cáo mới nhất
     */
    private LocalDateTime findLatestDate(List<Report> reports) {
        return reports.stream()
                .map(Report::getDateReport)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * Đánh giá mức độ rủi ro dựa trên số lượng báo cáo
     */
    private String assessRiskLevel(Integer reportCount) {
        if (reportCount == null || reportCount == 0) return RISK_LOW;
        if (reportCount >= 10) return RISK_HIGH;
        if (reportCount >= 3) return RISK_MEDIUM;
        return RISK_LOW;
    }

    /**
     * Xử lý danh sách URL bằng chứng
     */
    private List<String> processEvidenceUrls(List<Attachment> attachments) {
        return attachments.stream()
                .map(Attachment::getUrl)
                .filter(StringUtils::hasText)
                .map(this::normalizeUrl)
                .collect(Collectors.toList());
    }

    /**
     * Chuẩn hóa URL (thêm base URL nếu cần)
     */
    private String normalizeUrl(String url) {
        if (url.startsWith("http")) return url;
        String normalizedUrl = url.startsWith("/") ? url : "/" + url;
        return "" + normalizedUrl;
    }

    /**
     * Chuyển đổi danh sách Report thành ReportItemDTO
     */
    private List<SubjectDetailResponseDTO.ReportItemDTO> mapToReportItems(List<Report> reports) {
        return reports.stream()
                .map(this::mapToReportItem)
                .collect(Collectors.toList());
    }

    /**
     * Chuyển đổi một Report thành ReportItemDTO
     */
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

    /**
     * Tìm các đối tượng liên quan
     */
    private List<SubjectDetailResponseDTO.RelatedSubjectDTO> findRelatedSubjects(String info, Integer type) {
        try {
            List<TopScamItemResponseDTO> topItems = getTopItemsByType(type);

            return topItems.stream()
                    .filter(item -> !Objects.equals(item.getInfo(), info))  // Loại bỏ chính nó
                    .limit(RELATED_SUBJECTS_LIMIT)
                    .map(item -> mapToRelatedSubject(item, type))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting related subjects for info: {}, type: {}", info, type, e);
            return Collections.emptyList();
        }
    }

    /**
     * Lấy danh sách top theo loại
     */
    private List<TopScamItemResponseDTO> getTopItemsByType(Integer type) {
        return switch (type) {
            case 1 -> getTopPhoneScams();
            case 2 -> getTopBankScams();
            case 3 -> getTopUrlScams();
            default -> Collections.emptyList();
        };
    }

    /**
     * Chuyển đổi TopScamItem thành RelatedSubject
     */
    private SubjectDetailResponseDTO.RelatedSubjectDTO mapToRelatedSubject(TopScamItemResponseDTO item, Integer type) {
        return SubjectDetailResponseDTO.RelatedSubjectDTO.builder()
                .info(item.getInfo())
                .type(type)
                .description(item.getDescription())
                .riskLevel(item.getStatus())
                .build();
    }


    // ============================= HÀM TIỆN ÍCH - LẤY THỐNG KÊ =============================

    /**
     * Lấy thống kê phone hoặc tạo mặc định
     */
    private PhoneScamStats getPhoneStatsOrDefault(String phoneNumber) {
        return phoneRepository.findByPhoneNumber(phoneNumber)
                .orElse(PhoneScamStats.builder()
                        .viewCount(0)
                        .verifiedCount(0)
                        .build());
    }

    /**
     * Lấy thống kê bank hoặc tạo mặc định
     */
    private BankScamStats getBankStatsOrDefault(String bankAccount) {
        return bankRepository.findByBankAccount(bankAccount)
                .orElse(BankScamStats.builder()
                        .viewCount(0)
                        .verifiedCount(0)
                        .build());
    }

    /**
     * Lấy thống kê URL hoặc tạo mặc định
     */
    private UrlScamStats getUrlStatsOrDefault(String url) {
        return urlRepository.findByUrl(url)
                .orElse(UrlScamStats.builder()
                        .viewCount(0)
                        .verifiedCount(0)
                        .build());
    }


    // ============================= INNER CLASS - CONTAINER DỮ LIỆU =============================

    /**
     * Container chứa tất cả dữ liệu liên quan đến một subject
     * Immutable và lightweight để tối ưu hiệu suất
     *
     * Vai trò: Đóng gói dữ liệu để truyền giữa các method một cách an toàn
     */
    private static class SubjectDataBundle {
        private final String info;                          // Thông tin đối tượng (phone/bank/url)
        private final Integer type;                         // Loại đối tượng (1,2,3)
        private final List<ReportDetail> reportDetails;    // Chi tiết báo cáo
        private final List<Report> reports;                // Danh sách báo cáo
        private final List<Attachment> attachments;        // File đính kèm

        public SubjectDataBundle(String info, Integer type, List<ReportDetail> reportDetails,
                                 List<Report> reports, List<Attachment> attachments) {
            this.info = info;
            this.type = type;
            // Tạo bản sao bất biến để đảm bảo dữ liệu không thay đổi
            this.reportDetails = List.copyOf(reportDetails);
            this.reports = List.copyOf(reports);
            this.attachments = List.copyOf(attachments);
        }

        // Getter methods - chỉ đọc, không thể thay đổi
        public String getInfo() { return info; }
        public Integer getType() { return type; }
        public List<ReportDetail> getReportDetails() { return reportDetails; }
        public List<Report> getReports() { return reports; }
        public List<Attachment> getAttachments() { return attachments; }
    }
}