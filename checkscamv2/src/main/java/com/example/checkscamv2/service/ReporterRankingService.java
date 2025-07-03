package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.response.RankingPageResponseDTO;
import com.example.checkscamv2.dto.response.ReporterRankingResponseDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ReporterRankingService {
    
    RankingPageResponseDTO getReporterRanking(Pageable pageable);
    
    List<ReporterRankingResponseDTO> getTop3Reporters();
    
    Map<String, Object> getRankingStats();
    
    ReporterRankingResponseDTO getUserRanking(Long userId);
}
