package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.MonthlyReportStatsDTO;
import com.example.checkscamv2.dto.YearlyReportStatsDTO;
import com.example.checkscamv2.dto.request.CreateReportRequest;
import com.example.checkscamv2.dto.request.UpdateReportRequest;
import com.example.checkscamv2.dto.response.RankingPageResponseDTO;
import com.example.checkscamv2.dto.response.ReporterRankingResponseDTO;
import com.example.checkscamv2.entity.Attachment;
import com.example.checkscamv2.entity.Report;
import com.example.checkscamv2.exception.CheckScamException;
import com.example.checkscamv2.exception.DataNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ReportService {
    @Transactional
    List<Attachment> uploadFile(Long reportId, List<MultipartFile> files) throws Exception;
    Report createReport(CreateReportRequest request) throws Exception;
    Report getReportById(Long id) throws CheckScamException;
    Report updateReport(Long id, UpdateReportRequest request) throws CheckScamException;
    List<MonthlyReportStatsDTO> getMonthlyStats(Integer year);
    List<YearlyReportStatsDTO> getYearlyStats();
    Resource loadImage(String imageName) throws IOException;
    String getImageMimeType(String imageName) throws IOException;
    Page<Report> getAll(Pageable pageable);
    RankingPageResponseDTO getReporterRanking(int page, int size);
    List<ReporterRankingResponseDTO> getTop3Reporters();
    Map<String, Object> getRankingStats();

    // Phương thức để xác nhận báo cáo (chuyển trạng thái sang APPROVED)
    Report approveReport(Long reportId) throws DataNotFoundException; // KHÔNG THÊM categoryIdAfterHandle để đúng yêu cầu "chỉ trạng thái"

    // Phương thức để từ chối báo cáo (chuyển trạng thái sang REJECTED)
    Report rejectReport(Long reportId) throws DataNotFoundException;
}