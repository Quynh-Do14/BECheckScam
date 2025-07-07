package com.example.checkscamv2.controller;

import com.example.checkscamv2.dto.response.RankingPageResponseDTO;
import com.example.checkscamv2.dto.response.ReporterRankingResponseDTO;
import com.example.checkscamv2.dto.response.ResponseObject;
import com.example.checkscamv2.service.ReporterRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reporter-ranking")
@RequiredArgsConstructor
public class ReporterRankingController {

    private final ReporterRankingService reporterRankingService;

    @GetMapping
    public ResponseEntity<RankingPageResponseDTO> getReporterRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            RankingPageResponseDTO response = reporterRankingService.getReporterRanking(pageable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RankingPageResponseDTO.builder()
                    .reporters(List.of())
                    .currentPage(page)
                    .totalPages(0)
                    .totalElements(0L)
                    .pageSize(size)
                    .build());
        }
    }

    @GetMapping("/top3")
    public ResponseEntity<List<ReporterRankingResponseDTO>> getTop3Reporters() {
        try {
            List<ReporterRankingResponseDTO> top3 = reporterRankingService.getTop3Reporters();
            return ResponseEntity.ok(top3);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(List.of());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRankingStats() {
        try {
            Map<String, Object> stats = reporterRankingService.getRankingStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "totalReporters", 0,
                    "totalApprovedReports", 0,
                    "averageSuccessRate", 0.0
                ));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ResponseObject> getUserRanking(@PathVariable Long userId) {
        try {
            ReporterRankingResponseDTO userRanking = reporterRankingService.getUserRanking(userId);
            return ResponseEntity.ok(
                ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("User ranking retrieved successfully")
                    .data(userRanking)
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Error retrieving user ranking: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }
}
