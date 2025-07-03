package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.component.FileUtils;
import com.example.checkscamv2.constant.ErrorCodeEnum;
import com.example.checkscamv2.dto.MonthlyReportStatsDTO;
import com.example.checkscamv2.dto.ReportDetailDTO;
import com.example.checkscamv2.dto.YearlyReportStatsDTO;
import com.example.checkscamv2.dto.request.CreateReportRequest;
import com.example.checkscamv2.dto.request.UpdateReportRequest;
import com.example.checkscamv2.dto.response.RankingPageResponseDTO;
import com.example.checkscamv2.dto.response.ReporterRankingResponseDTO;
import com.example.checkscamv2.entity.Attachment;
import com.example.checkscamv2.entity.Category;
import com.example.checkscamv2.entity.Report;
import com.example.checkscamv2.entity.ReportDetail;
import com.example.checkscamv2.entity.PhoneScamStats;
import com.example.checkscamv2.entity.BankScamStats;
import com.example.checkscamv2.entity.UrlScamStats;
import com.example.checkscamv2.exception.CheckScamException;
import com.example.checkscamv2.exception.DataNotFoundException;
import com.example.checkscamv2.exception.FileUploadValidationException;
import com.example.checkscamv2.repository.AttachmentRepository;
import com.example.checkscamv2.repository.CategoryRepository;
import com.example.checkscamv2.repository.ReportRepository;
import com.example.checkscamv2.repository.ReportDetailRepository;
import com.example.checkscamv2.repository.PhoneScamRepository;
import com.example.checkscamv2.repository.BankScamRepository;
import com.example.checkscamv2.repository.UrlScamRepository;
import com.example.checkscamv2.repository.PhoneScamStatsRepository;
import com.example.checkscamv2.repository.BankScamStatsRepository;
import com.example.checkscamv2.repository.UrlScamStatsRepository;
import com.example.checkscamv2.service.ReportService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;
    private final AttachmentRepository attachmentRepository;
    private final FileUtils fileUtils;
    private final CategoryRepository categoryRepository;
    private final ReportDetailRepository reportDetailRepository;
    private final PhoneScamRepository phoneScamRepository;
    private final BankScamRepository bankScamRepository;
    private final UrlScamRepository urlScamRepository;
    private final PhoneScamStatsRepository phoneScamStatsRepository;
    private final BankScamStatsRepository bankScamStatsRepository;
    private final UrlScamStatsRepository urlScamStatsRepository;

    @Transactional
    @Override
    public Report createReport(CreateReportRequest request) throws Exception {
        if (request.getReportDetails() == null || request.getReportDetails().isEmpty()) {
            throw new IllegalArgumentException("Phải có ít nhất một thông tin báo cáo");
        }
        if (request.getEmailAuthorReport() == null || request.getEmailAuthorReport().isBlank()) {
            throw new IllegalArgumentException("Email người báo cáo là bắt buộc");
        }
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new DataNotFoundException("Danh mục không tồn tại"));

        Report newReport = Report.builder()
                .emailAuthorReport(request.getEmailAuthorReport())
                .description(request.getDescription())
                .moneyScam(request.getMoneyScam())
                .dateReport(LocalDateTime.now())
                .status(1) // GIỮ NGUYÊN: Set trạng thái mặc định là PENDING (1) cho trường 'status'
                .category(category)
                .reportDetails(new ArrayList<>())
                .attachments(new ArrayList<>())
                .build();

        for (ReportDetailDTO detailDTO : request.getReportDetails()) {
            ReportDetail detail = ReportDetail.builder()
                    .type(detailDTO.getType())
                    .info(detailDTO.getInfo())
                    .description(detailDTO.getDescription())
                    .status(1) // GIỮ NGUYÊN: Trường 'status' trong ReportDetail, không liên quan
                    .report(newReport)
                    .build();
            newReport.getReportDetails().add(detail);
        }
        newReport = reportRepository.save(newReport);
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            List<Attachment> attachments = uploadFile(newReport.getId(), request.getAttachments());
            newReport.setAttachments(attachments);
        }
        return newReport;
    }

    @Override
    public Report getReportById(Long id) throws CheckScamException {
        return reportRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy báo cáo với id: "+id));
    }

    @Override
    public Report updateReport(Long id, UpdateReportRequest request) throws CheckScamException {
        // GIỮ NGUYÊN LOGIC NÀY VÌ KHÔNG LIÊN QUAN ĐẾN TRẠNG THÁI XỬ LÝ
        return null;
    }

    @Transactional
    @Override
    public List<Attachment> uploadFile(Long reportId, List<MultipartFile> files) throws Exception {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CheckScamException(ErrorCodeEnum.NOT_FOUND));

        List<MultipartFile> validFiles =
                (files == null ? List.<MultipartFile>of() : files).stream()
                        .filter(f -> f != null && !f.isEmpty() && f.getSize() > 0)
                        .toList();

        if (validFiles.isEmpty()) {
            return List.of();
        }

        List<Attachment> saved = new ArrayList<>();

        for (MultipartFile file : validFiles) {
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new FileUploadValidationException(
                        "Kích thước file vượt quá 10MB: " + file.getOriginalFilename(),
                        HttpStatus.PAYLOAD_TOO_LARGE);
            }
            if (file.getContentType() == null
                    || !file.getContentType().startsWith("image/")) {
                throw new FileUploadValidationException(
                        "File phải là hình ảnh: " + file.getOriginalFilename(),
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }

            String storedName = fileUtils.storeFile(file);

            Attachment toSave = Attachment.builder()
                    .report(report)
                    .url(storedName)
                    .build();

            saved.add(attachmentRepository.save(toSave));
        }
        return saved;
    }

    @Override
    public Resource loadImage(String imageName) throws IOException {
        validateImageName(imageName);
        Path imagePath = fileUtils.resolve(imageName);

        if (!Files.exists(imagePath)) {
            Path fallback = fileUtils.resolve("notfound.jpeg");
            if (Files.exists(fallback)) {
                return new UrlResource(fallback.toUri());
            }
            throw new FileNotFoundException("Image not found: " + imageName);
        }
        return new UrlResource(imagePath.toUri());
    }

    @Override
    public String getImageMimeType(String imageName) throws IOException {
        Path path = fileUtils.resolve(imageName);
        if (!Files.exists(path)) {
            path = fileUtils.resolve("notfound.jpeg");
        }
        return Files.probeContentType(path);
    }

    private void validateImageName(String imageName) {
        if (imageName.contains("..") || imageName.contains("/") || imageName.contains("\\")) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }
        if (!imageName.matches("(?i).*\\.(jpg|jpeg|png)$")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file .jpg, .jpeg, .png");
        }
    }

    @Override
    public List<MonthlyReportStatsDTO> getMonthlyStats(Integer year) {
        return reportRepository.findReportCountByMonth(year);
    }

    @Override
    public List<YearlyReportStatsDTO> getYearlyStats() {
        List<Object[]> results = reportRepository.findReportCountByYear();
        List<YearlyReportStatsDTO> stats = new ArrayList<>();
        for (Object[] row : results) {
            if (row[0] != null && row[1] != null) {
                int year = ((Number) row[0]).intValue();
                long count = ((Number) row[1]).longValue();
                stats.add(new YearlyReportStatsDTO(year, count));
            }
        }
        return stats;
    }

    @Override
    public RankingPageResponseDTO getReporterRanking(int page, int size) {
        // Lấy tất cả dữ liệu và tự phân trang
        List<Object[]> allData = reportRepository.findAllReporterRankingData(); // Đã sửa query trong repo

        // Tính toán phân trang
        int totalElements = allData.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);

        List<ReporterRankingResponseDTO> reporters = new ArrayList<>();

        for (int i = startIndex; i < endIndex; i++) {
            Object[] row = allData.get(i);
            String email = (String) row[0];
            // Lấy APPROVED_COUNT từ report.status = 2
            Long approvedCount = ((Number) row[1]).longValue();
            Long totalCount = ((Number) row[2]).longValue();
            LocalDateTime firstReport = (LocalDateTime) row[3];
            LocalDateTime lastReport = (LocalDateTime) row[4];

            double successRate = totalCount > 0 ? (approvedCount * 100.0 / totalCount) : 0.0;

            ReporterRankingResponseDTO dto = ReporterRankingResponseDTO.builder()
                    .id((long) (i + 1))
                    .email(email)
                    .totalReports(Math.toIntExact(totalCount))
                    .approvedReports(Math.toIntExact(approvedCount))
                    .successRate(Math.round(successRate * 100.0) / 100.0)
                    .rank(i + 1)
                    .lastReportDate(lastReport.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();
            reporters.add(dto);
        }

        return new RankingPageResponseDTO(
                reporters,
                page,
                totalPages,
                (long) totalElements,
                size
        );
    }

    @Override
    public List<ReporterRankingResponseDTO> getTop3Reporters() {
        List<Object[]> allData = reportRepository.findAllReporterRankingData();
        List<ReporterRankingResponseDTO> top3 = new ArrayList<>();

        // Lấy top 3
        int limit = Math.min(3, allData.size());
        for (int i = 0; i < limit; i++) {
            Object[] row = allData.get(i);
            String email = (String) row[0];
            // Lấy APPROVED_COUNT từ report.status = 2
            Long approvedCount = ((Number) row[1]).longValue();
            Long totalCount = ((Number) row[2]).longValue();
            LocalDateTime firstReport = (LocalDateTime) row[3];
            LocalDateTime lastReport = (LocalDateTime) row[4];

            double successRate = totalCount > 0 ? (approvedCount * 100.0 / totalCount) : 0.0;

            ReporterRankingResponseDTO dto = ReporterRankingResponseDTO.builder()
                    .id((long) (i + 1))
                    .email(email)
                    .totalReports(Math.toIntExact(totalCount))
                    .approvedReports(Math.toIntExact(approvedCount))
                    .successRate(Math.round(successRate * 100.0) / 100.0)
                    .rank(i + 1)
                    .lastReportDate(lastReport.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();
            top3.add(dto);
        }

        return top3;
    }

    @Override
    public Map<String, Object> getRankingStats() {
        Long totalReporters = reportRepository.countTotalReporters();
        Long totalApprovedReports = reportRepository.countTotalApprovedReports();

        List<Object[]> stats = reportRepository.findAllReporterStats();
        double averageSuccessRate = 0.0;

        if (!stats.isEmpty()) {
            double totalSuccessRate = 0.0;
            for (Object[] row : stats) {
                Long approvedCount = ((Number) row[1]).longValue();
                Long totalCount = ((Number) row[2]).longValue();
                if (totalCount > 0) {
                    totalSuccessRate += (approvedCount * 100.0 / totalCount);
                }
            }
            averageSuccessRate = totalSuccessRate / stats.size();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalReporters", totalReporters != null ? totalReporters : 0L);
        result.put("totalApprovedReports", totalApprovedReports != null ? totalApprovedReports : 0L);
        result.put("averageSuccessRate", Math.round(averageSuccessRate * 100.0) / 100.0);

        return result;
    }

    @Override
    public Page<Report> getAll(Pageable pageable) {
        return reportRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Report approveReport(Long reportId) throws DataNotFoundException {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy báo cáo với id: " + reportId));

        report.setStatus(2);
        // Cập nhật tất cả report detail status = 2
        List<ReportDetail> details = reportDetailRepository.findByReportId(reportId);
        for (ReportDetail detail : details) {
            detail.setStatus(2);
            if (detail.getType() == 1) { // SDT
                var phoneScamOpt = phoneScamRepository.findByPhoneNumber(detail.getInfo());
                var phoneScam = phoneScamOpt.orElseGet(() -> {
                    var newPhone = com.example.checkscamv2.entity.PhoneScam.builder()
                            .phoneNumber(detail.getInfo())
                            .ownerName("")
                            .build();
                    var saved = phoneScamRepository.save(newPhone);
                    phoneScamRepository.flush();
                    return saved;
                });
                if (phoneScam.getId() == null) {
                    phoneScamRepository.flush();
                }
                phoneScam = phoneScamRepository.findById(phoneScam.getId()).orElseThrow();
                PhoneScamStats stats = phoneScamStatsRepository.findById(phoneScam.getId()).orElse(null);
                if (stats == null) {
                    stats = new PhoneScamStats();
                    stats.setId(phoneScam.getId());
                    stats.setPhoneScam(phoneScam);
                    stats.setVerifiedCount(1);
                    stats.setLastReportAt(LocalDateTime.now());
                    stats.setViewCount(0);
                } else {
                    stats.setVerifiedCount(stats.getVerifiedCount() == null ? 1 : stats.getVerifiedCount() + 1);
                    stats.setLastReportAt(LocalDateTime.now());
                }
                phoneScamStatsRepository.save(stats);
            } else if (detail.getType() == 2) { // STK
                var bankScamOpt = bankScamRepository.findByBankAccount(detail.getInfo());
                var bankScam = bankScamOpt.orElseGet(() -> {
                    var newBank = com.example.checkscamv2.entity.BankScam.builder()
                            .bankAccount(detail.getInfo())
                            .bankName("")
                            .nameAccount("")
                            .build();
                    var saved = bankScamRepository.save(newBank);
                    bankScamRepository.flush();
                    return saved;
                });
                if (bankScam.getId() == null) {
                    bankScamRepository.flush();
                }
                bankScam = bankScamRepository.findById(bankScam.getId()).orElseThrow();
                BankScamStats stats = bankScamStatsRepository.findById(bankScam.getId()).orElse(null);
                if (stats == null) {
                    stats = new BankScamStats();
                    stats.setId(bankScam.getId());
                    stats.setBankScam(bankScam);
                    stats.setVerifiedCount(1);
                    stats.setLastReportAt(LocalDateTime.now());
                    stats.setViewCount(0);
                } else {
                    stats.setVerifiedCount(stats.getVerifiedCount() == null ? 1 : stats.getVerifiedCount() + 1);
                    stats.setLastReportAt(LocalDateTime.now());
                }
                bankScamStatsRepository.save(stats);
            } else if (detail.getType() == 3) { // URL
                var urlScamOpt = urlScamRepository.findByUrl(detail.getInfo());
                var urlScam = urlScamOpt.orElseGet(() -> {
                    var newUrl = com.example.checkscamv2.entity.UrlScam.builder()
                            .url(detail.getInfo())
                            .build();
                    var saved = urlScamRepository.save(newUrl);
                    urlScamRepository.flush();
                    return saved;
                });
                if (urlScam.getId() == null) {
                    urlScamRepository.flush();
                }
                urlScam = urlScamRepository.findById(urlScam.getId()).orElseThrow();
                UrlScamStats stats = urlScamStatsRepository.findById(urlScam.getId()).orElse(null);
                if (stats == null) {
                    stats = new UrlScamStats();
                    stats.setId(urlScam.getId());
                    stats.setUrlScam(urlScam);
                    stats.setVerifiedCount(1);
                    stats.setLastReportAt(LocalDateTime.now());
                    stats.setViewCount(0);
                } else {
                    stats.setVerifiedCount(stats.getVerifiedCount() == null ? 1 : stats.getVerifiedCount() + 1);
                    stats.setLastReportAt(LocalDateTime.now());
                }
                urlScamStatsRepository.save(stats);
            }
        }
        reportDetailRepository.saveAll(details);
        return reportRepository.save(report);
    }

    @Override
    @Transactional
    public Report rejectReport(Long reportId) throws DataNotFoundException {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy báo cáo với id: " + reportId));

        report.setStatus(3);
        return reportRepository.save(report);
    }
}