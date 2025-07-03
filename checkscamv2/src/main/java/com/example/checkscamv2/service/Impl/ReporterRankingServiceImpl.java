package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.dto.response.RankingPageResponseDTO;
import com.example.checkscamv2.dto.response.ReporterRankingResponseDTO;
import com.example.checkscamv2.entity.User;
import com.example.checkscamv2.repository.ReportRepository;
import com.example.checkscamv2.repository.UserRepository;
import com.example.checkscamv2.service.ReporterRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ReporterRankingServiceImpl implements ReporterRankingService {

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    @Override
    public RankingPageResponseDTO getReporterRanking(Pageable pageable) {
        Page<User> usersPage = userRepository.findAll(pageable);
        
        List<ReporterRankingResponseDTO> reporters = IntStream.range(0, usersPage.getContent().size())
            .mapToObj(i -> {
                User user = usersPage.getContent().get(i);
                long totalReports = reportRepository.countByEmail(user.getEmail());
                long approvedReports = reportRepository.countByEmailAndStatus(user.getEmail(), 2); // 2 = APPROVED
                double successRate = totalReports > 0 ? (double) approvedReports / totalReports * 100 : 0.0;
                int rank = (pageable.getPageNumber() * pageable.getPageSize()) + i + 1;
                
                return ReporterRankingResponseDTO.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .totalReports(Math.toIntExact(totalReports))
                    .approvedReports(Math.toIntExact(approvedReports))
                    .successRate(successRate)
                    .rank(rank)
                    .lastReportDate(getLastReportDate(user.getId()))
                    .build();
            })
            .toList();

        return RankingPageResponseDTO.builder()
            .reporters(reporters)
            .currentPage(usersPage.getNumber())
            .totalPages(usersPage.getTotalPages())
            .totalElements(usersPage.getTotalElements())
            .pageSize(usersPage.getSize())
            .build();
    }

    @Override
    public List<ReporterRankingResponseDTO> getTop3Reporters() {
        Pageable top3 = PageRequest.of(0, 3);
        Page<User> topUsersPage = userRepository.findAll(top3);
        List<User> topUsers = topUsersPage.getContent();
        
        return IntStream.range(0, Math.min(3, topUsers.size()))
            .mapToObj(i -> {
                User user = topUsers.get(i);
                long totalReports = reportRepository.countByEmail(user.getEmail());
                long approvedReports = reportRepository.countByEmailAndStatus(user.getEmail(), 2); // 2 = APPROVED
                double successRate = totalReports > 0 ? (double) approvedReports / totalReports * 100 : 0.0;
                
                return ReporterRankingResponseDTO.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .totalReports(Math.toIntExact(totalReports))
                    .approvedReports(Math.toIntExact(approvedReports))
                    .successRate(successRate)
                    .rank(i + 1)
                    .lastReportDate(getLastReportDate(user.getId()))
                    .build();
            })
            .toList();
    }

    @Override
    public Map<String, Object> getRankingStats() {
        long totalReporters = userRepository.count();
        long totalApprovedReports = reportRepository.countByStatus(2); // 2 = APPROVED
        long totalReports = reportRepository.count();
        double averageSuccessRate = totalReports > 0 ? (double) totalApprovedReports / totalReports * 100 : 0.0;

        return Map.of(
            "totalReporters", totalReporters,
            "totalApprovedReports", totalApprovedReports,
            "averageSuccessRate", Math.round(averageSuccessRate * 10.0) / 10.0
        );
    }

    @Override
    public ReporterRankingResponseDTO getUserRanking(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        long totalReports = reportRepository.countByEmail(user.getEmail());
        long approvedReports = reportRepository.countByEmailAndStatus(user.getEmail(), 2); // 2 = APPROVED
        double successRate = totalReports > 0 ? (double) approvedReports / totalReports * 100 : 0.0;
        
        // Calculate rank (simplified - can be optimized with proper query)
        List<User> allUsers = userRepository.findAll();
        int rank = 1; // This should be calculated based on actual ranking logic
        
        return ReporterRankingResponseDTO.builder()
            .id(user.getId())
            .email(user.getEmail())
            .totalReports(Math.toIntExact(totalReports))
            .approvedReports(Math.toIntExact(approvedReports))
            .successRate(successRate)
            .rank(rank)
            .lastReportDate(getLastReportDate(userId))
            .build();
    }

    private String getLastReportDate(Long userId) {
        // This should return the actual last report date from database
        // For now, return a placeholder
        return LocalDateTime.now().minusDays(1).toString();
    }
}
