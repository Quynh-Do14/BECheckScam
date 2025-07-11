package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.dto.response.SubjectDetailResponseDTO;
import com.example.checkscamv2.entity.*;
import com.example.checkscamv2.repository.*;
import com.example.checkscamv2.service.ScamStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.transaction.annotation.Transactional;
import com.example.checkscamv2.dto.response.TopScamItemResponseDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScamStatsServiceImpl implements ScamStatsService {
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


    @Override
    public List<TopScamItemResponseDTO> getTopPhoneScams() {
        return phoneRepository.getTopPhonesByViews();
    }
    
    @Override
    public List<TopScamItemResponseDTO> getTopBankScams() {
        return bankRepository.getTopBanksByViews();
    }
    
    @Override
    public List<TopScamItemResponseDTO> getTopUrlScams() {
        return urlRepository.getTopUrlsByViews();
    }
    
    @Override
    @Transactional
    public void incrementPhoneViewCount(String phoneNumber) {
        try {
            phoneRepository.findByPhoneNumber(phoneNumber)
                .ifPresentOrElse(stats -> {
                    stats.setViewCount(stats.getViewCount() + 1);
                    phoneRepository.save(stats);
                }, () -> {
                    phoneScamRepository.findByPhoneNumber(phoneNumber)
                        .ifPresent(phoneScam -> {
                            PhoneScamStats newStats = PhoneScamStats.builder()
                                .id(phoneScam.getId())
                                .phoneScam(phoneScam)
                                .verifiedCount(0)
                                .viewCount(1)
                                .build();
                            phoneRepository.save(newStats);
                        });
                });
        } catch (Exception e) {
            System.err.println("Error incrementing phone view count: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void incrementBankViewCount(String bankAccount) {
        bankRepository.findByBankAccount(bankAccount)
            .ifPresent(stats -> {
                stats.setViewCount(stats.getViewCount() + 1);
                bankRepository.save(stats);
            });
    }
    
    @Override
    @Transactional
    public void incrementUrlViewCount(String url) {
        urlRepository.findByUrl(url)
            .ifPresent(stats -> {
                stats.setViewCount(stats.getViewCount() + 1);
                urlRepository.save(stats);
            });
    }
    
    @Override
    public SubjectDetailResponseDTO getSubjectDetail(String info, Integer type) {
        try {
            // Get report details for this subject
            List<ReportDetail> reportDetails = reportDetailRepository.findByInfoAndType(info, type);
            
            if (reportDetails.isEmpty()) {
                // Fallback: kiểm tra trong bảng scam tương ứng
                return createFallbackResponse(info, type);
            }
            
            // Get all reports related to this subject
            List<Long> reportIds = reportDetails.stream()
                .map(rd -> rd.getReport().getId())
                .distinct()
                .collect(Collectors.toList());
            
            List<Report> reports = reportRepository.findAllById(reportIds);
            
            // Get attachments/evidence
            List<Attachment> attachments = attachmentRepository.findByReportIdIn(reportIds);
            List<String> evidenceImages = attachments.stream()
                .map(a -> {
                    String url = a.getUrl();
                    if (url != null && !url.startsWith("http")) {
                        return "http://localhost:8080" + (url.startsWith("/") ? url : "/" + url);
                    }
                    return url;
                })
                .filter(url -> url != null)
                .collect(Collectors.toList());
            
            long totalScamAmount = reports.stream()
                .filter(r -> r.getMoneyScam() != null)
                .mapToLong(r -> {
                    try {
                        return Long.parseLong(r.getMoneyScam().replaceAll("[^0-9]", ""));
                    } catch (NumberFormatException e) {
                        return 0L;
                    }
                })
                .sum();
            
            String subjectName = getSubjectName(info, type);
            
            List<SubjectDetailResponseDTO.ReportItemDTO> reportItems = reports.stream()
                .map(this::buildReportItem)
                .collect(Collectors.toList());
            
            List<SubjectDetailResponseDTO.RelatedSubjectDTO> relatedSubjects = getRelatedSubjects(info, type);
            
            return SubjectDetailResponseDTO.builder()
                .info(info)
                .type(type)
                .name(subjectName)
                .description(getSubjectDescription(reportDetails))
                .totalScamAmount(totalScamAmount)
                .reportCount(reports.size())
                .lastReportDate(reports.stream()
                    .map(Report::getDateReport)
                    .max(java.util.Comparator.naturalOrder())
                    .orElse(null))
                .riskLevel(determineRiskLevel(reports.size()))
                .evidenceImages(evidenceImages)
                .reports(reportItems)
                .relatedSubjects(relatedSubjects)
                .build();
                
        } catch (Exception e) {
            System.err.println("Error getting subject detail: " + e.getMessage());
            return null;
        }
    }
    
    private String getSubjectName(String info, Integer type) {
        switch (type) {
            case 1: // Phone
                return phoneScamRepository.findByPhoneNumber(info)
                    .map(PhoneScam::getOwnerName)
                    .orElse("Số điện thoại nghi vấn");
            case 2: // Bank
                return bankScamRepository.findByBankAccount(info)
                    .map(bank -> (bank.getBankName() != null ? bank.getBankName() + " - " : "") +
                               (bank.getNameAccount() != null ? bank.getNameAccount() : "Tài khoản nghi vấn"))
                    .orElse("Tài khoản ngân hàng nghi vấn");
            case 3: // URL
                return "Website nghi vấn";
            default:
                return "Đối tượng không xác định";
        }
    }
    
    private String getSubjectDescription(List<ReportDetail> reportDetails) {
        return reportDetails.stream()
            .map(ReportDetail::getDescription)
            .filter(desc -> desc != null && !desc.trim().isEmpty())
            .findFirst()
            .orElse("Thông tin chi tiết về đối tượng này đang được cập nhật.");
    }
    
    private SubjectDetailResponseDTO.ReportItemDTO buildReportItem(Report report) {
        long amount = 0;
        if (report.getMoneyScam() != null) {
            try {
                amount = Long.parseLong(report.getMoneyScam().replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                // Keep amount as 0
            }
        }
        
        return SubjectDetailResponseDTO.ReportItemDTO.builder()
            .id(report.getId())
            .date(report.getDateReport())
            .description(report.getDescription() != null ? report.getDescription() : "Báo cáo lừa đảo")
            .amount(amount)
            .reporterLocation("Việt Nam")
            .status(getReportStatus(report.getStatus()))
            .build();
    }
    
    private String getReportStatus(Integer status) {
        if (status == null) return "Đang xử lý";
        switch (status) {
            case 1: return "Đã xác minh";
            case 2: return "Đã từ chối";
            default: return "Đang xử lý";
        }
    }
    

    
    private List<SubjectDetailResponseDTO.RelatedSubjectDTO> getRelatedSubjects(String info, Integer type) {
        // Simplified implementation - you can enhance this with more sophisticated logic
        List<SubjectDetailResponseDTO.RelatedSubjectDTO> related = new ArrayList<>();
        
        // For now, just return a few related items from the same type
        List<TopScamItemResponseDTO> topItems;
        switch (type) {
            case 1:
                topItems = getTopPhoneScams();
                break;
            case 2:
                topItems = getTopBankScams();
                break;
            case 3:
                topItems = getTopUrlScams();
                break;
            default:
                return related;
        }
        
        return topItems.stream()
            .filter(item -> !item.getInfo().equals(info)) // Exclude current subject
            .limit(3) // Get top 3 related
            .map(item -> SubjectDetailResponseDTO.RelatedSubjectDTO.builder()
                .info(item.getInfo())
                .type(type)
                .description(item.getDescription())
                .riskLevel(item.getStatus())
                .build())
            .collect(Collectors.toList());
    }
    
    private String determineRiskLevel(Integer reportCount) {
        if (reportCount == null || reportCount == 0) return "low";
        if (reportCount >= 10) return "high";
        if (reportCount >= 3) return "medium";
        return "low";
    }
    
    /**
     * Tạo response mặc định khi không tìm thấy report_detail
     * nhưng có dữ liệu trong bảng scam tương ứng
     */
    private SubjectDetailResponseDTO createFallbackResponse(String info, Integer type) {
        switch (type) {
            case 1: // Phone
                return createPhoneFallbackResponse(info);
            case 2: // Bank
                return createBankFallbackResponse(info);
            case 3: // URL
                return createUrlFallbackResponse(info);
            default:
                return null;
        }
    }
    
    /**
     * Tạo fallback response cho Phone Scam
     */
    private SubjectDetailResponseDTO createPhoneFallbackResponse(String phoneNumber) {
        Optional<PhoneScam> phoneScamOpt = phoneScamRepository.findByPhoneNumber(phoneNumber);
        
        if (phoneScamOpt.isEmpty()) {
            return null;
        }
        
        PhoneScam phoneScam = phoneScamOpt.get();
        
        // Lấy thông tin từ stats nếu có
        Optional<PhoneScamStats> statsOpt = phoneRepository.findByPhoneNumber(phoneNumber);
        int viewCount = statsOpt.map(PhoneScamStats::getViewCount).orElse(0);
        int verifiedCount = statsOpt.map(PhoneScamStats::getVerifiedCount).orElse(0);
        
        String subjectName = phoneScam.getOwnerName() != null ? 
            phoneScam.getOwnerName() : "Số điện thoại nghi vấn";
            
        return SubjectDetailResponseDTO.builder()
            .info(phoneNumber)
            .type(1)
            .name(subjectName)
            .description("Đây là số điện thoại được ghi nhận trong hệ thống nhưng chưa có báo cáo chi tiết từ người dùng.")
            .totalScamAmount(0L)
            .reportCount(verifiedCount)
            .lastReportDate(null)
            .riskLevel("low")
            .evidenceImages(new ArrayList<>())
            .reports(new ArrayList<>())
            .relatedSubjects(getRelatedSubjects(phoneNumber, 1))
            .build();
    }
    
    /**
     * Tạo fallback response cho Bank Scam
     */
    private SubjectDetailResponseDTO createBankFallbackResponse(String bankAccount) {
        Optional<BankScam> bankScamOpt = bankScamRepository.findByBankAccount(bankAccount);
        
        if (bankScamOpt.isEmpty()) {
            return null;
        }
        
        BankScam bankScam = bankScamOpt.get();
        
        // Lấy thông tin từ stats nếu có
        Optional<BankScamStats> statsOpt = bankRepository.findByBankAccount(bankAccount);
        int viewCount = statsOpt.map(BankScamStats::getViewCount).orElse(0);
        int verifiedCount = statsOpt.map(BankScamStats::getVerifiedCount).orElse(0);
        
        String subjectName = (bankScam.getBankName() != null ? bankScam.getBankName() + " - " : "") +
                           (bankScam.getNameAccount() != null ? bankScam.getNameAccount() : "Tài khoản nghi vấn");
            
        return SubjectDetailResponseDTO.builder()
            .info(bankAccount)
            .type(2)
            .name(subjectName)
            .description("Đây là tài khoản ngân hàng được ghi nhận trong hệ thống nhưng chưa có báo cáo chi tiết từ người dùng.")
            .totalScamAmount(0L)
            .reportCount(verifiedCount)
            .lastReportDate(null)
            .riskLevel("low")
            .evidenceImages(new ArrayList<>())
            .reports(new ArrayList<>())
            .relatedSubjects(getRelatedSubjects(bankAccount, 2))
            .build();
    }
    
    /**
     * Tạo fallback response cho URL Scam
     */
    private SubjectDetailResponseDTO createUrlFallbackResponse(String url) {
        Optional<UrlScam> urlScamOpt = urlScamRepository.findByUrl(url);
        
        if (urlScamOpt.isEmpty()) {
            return null;
        }
        
        UrlScam urlScam = urlScamOpt.get();
        
        // Lấy thông tin từ stats nếu có
        Optional<UrlScamStats> statsOpt = urlRepository.findByUrl(url);
        int viewCount = statsOpt.map(UrlScamStats::getViewCount).orElse(0);
        int verifiedCount = statsOpt.map(UrlScamStats::getVerifiedCount).orElse(0);
            
        return SubjectDetailResponseDTO.builder()
            .info(url)
            .type(3)
            .name("Website nghi vấn")
            .description("Đây là website được ghi nhận trong hệ thống nhưng chưa có báo cáo chi tiết từ người dùng.")
            .totalScamAmount(0L)
            .reportCount(verifiedCount)
            .lastReportDate(null)
            .riskLevel("low")
            .evidenceImages(new ArrayList<>())
            .reports(new ArrayList<>())
            .relatedSubjects(getRelatedSubjects(url, 3))
            .build();
    }

}
