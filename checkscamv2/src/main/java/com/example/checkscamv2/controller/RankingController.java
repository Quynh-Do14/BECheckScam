package com.example.checkscamv2.controller;

import com.example.checkscamv2.dto.response.ResponseObject;
import com.example.checkscamv2.dto.response.SubjectDetailResponseDTO;
import com.example.checkscamv2.dto.response.TopScamItemResponseDTO;
import com.example.checkscamv2.service.ScamStatsService;
import com.example.checkscamv2.repository.ReportDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ranking")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RankingController {

    private final ScamStatsService scamStatsService;
    private final ReportDetailRepository reportDetailRepository;

    @GetMapping("/test-simple")
    public ResponseEntity<String> testSimple() {
        return ResponseEntity.ok("API is working!");
    }

    @GetMapping("/test-db")
    public ResponseEntity<ResponseObject> testDatabase() {
        try {
            // Đếm tổng số records trong database
            long totalReportDetails = reportDetailRepository.count();
            
            // Lấy vài records đầu tiên
            var sampleData = reportDetailRepository.findAll().stream()
                .limit(5)
                .map(rd -> Map.of(
                    "id", rd.getId(),
                    "info", rd.getInfo(),
                    "type", rd.getType(),
                    "description", rd.getDescription() != null ? rd.getDescription() : "null"
                ))
                .collect(java.util.stream.Collectors.toList());
            
            var result = Map.of(
                "totalRecords", totalReportDetails,
                "sampleData", sampleData
            );
            
            return ResponseEntity.ok(
                ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Database test successful")
                    .data(result)
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Database test failed: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @GetMapping("/test-data/{info}")
    public ResponseEntity<ResponseObject> testData(@PathVariable String info) {
        try {
            // Test các loại query khác nhau
            var phoneReports = reportDetailRepository.findByInfoAndType(info, 1);
            var bankReports = reportDetailRepository.findByInfoAndType(info, 2);
            var urlReports = reportDetailRepository.findByInfoAndType(info, 3);
            
            var testResult = Map.of(
                "info", info,
                "phoneReports", phoneReports.size(),
                "bankReports", bankReports.size(),
                "urlReports", urlReports.size(),
                "totalReports", phoneReports.size() + bankReports.size() + urlReports.size()
            );
            
            return ResponseEntity.ok(
                ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Test data retrieved")
                    .data(testResult)
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Error testing data: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @GetMapping("/top-phones")
    public ResponseEntity<ResponseObject> getTopPhones() {
        try {
            List<TopScamItemResponseDTO> topPhones = scamStatsService.getTopPhoneScams();
            return ResponseEntity.ok(
                ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Top phone scams retrieved successfully")
                    .data(topPhones)
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Error retrieving top phone scams: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @PostMapping("/increment-view")
    public ResponseEntity<ResponseObject> incrementViewCount(@RequestBody Map<String, Object> request) {
        try {
            String info = (String) request.get("info");
            Integer type = (Integer) request.get("type");
            
            if (info == null || type == null) {
                return ResponseEntity.badRequest()
                    .body(ResponseObject.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("Missing required parameters: info and type")
                        .data(null)
                        .build());
            }
            
            // Tăng view count dựa trên type
            switch (type) {
                case 1: // Phone
                    scamStatsService.incrementPhoneViewCount(info);
                    break;
                case 2: // Bank
                    scamStatsService.incrementBankViewCount(info);
                    break;
                case 3: // URL
                    scamStatsService.incrementUrlViewCount(info);
                    break;
                default:
                    return ResponseEntity.badRequest()
                        .body(ResponseObject.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .message("Invalid type. Must be 1 (phone), 2 (bank), or 3 (url)")
                            .data(null)
                            .build());
            }
            
            return ResponseEntity.ok(
                ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("View count incremented successfully")
                    .data(null)
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Error incrementing view count: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @GetMapping("/subject-detail/{info}")
    public ResponseEntity<ResponseObject> getSubjectDetail(
            @PathVariable String info,
            @RequestParam(required = false, defaultValue = "1") Integer type) {
        try {
            SubjectDetailResponseDTO subjectDetail = scamStatsService.getSubjectDetail(info, type);
            if (subjectDetail == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseObject.builder()
                        .status(HttpStatus.NOT_FOUND)
                        .message("Subject not found: " + info)
                        .data(null)
                        .build());
            }
            
            return ResponseEntity.ok(
                ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Subject detail retrieved successfully")
                    .data(subjectDetail)
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Error retrieving subject detail: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @GetMapping("/top-banks")
    public ResponseEntity<ResponseObject> getTopBanks() {
        try {
            List<TopScamItemResponseDTO> topBanks = scamStatsService.getTopBankScams();
            return ResponseEntity.ok(
                ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Top bank scams retrieved successfully")
                    .data(topBanks)
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Error retrieving top bank scams: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @GetMapping("/top-urls")
    public ResponseEntity<ResponseObject> getTopUrls() {
        try {
            List<TopScamItemResponseDTO> topUrls = scamStatsService.getTopUrlScams();
            return ResponseEntity.ok(
                ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Top URL scams retrieved successfully")
                    .data(topUrls)
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Error retrieving top URL scams: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @GetMapping("/top-all")
    public ResponseEntity<ResponseObject> getTopAll() {
        try {
            var response = Map.of(
                "phones", scamStatsService.getTopPhoneScams(),
                "banks", scamStatsService.getTopBankScams(),
                "urls", scamStatsService.getTopUrlScams()
            );
            
            return ResponseEntity.ok(
                ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("All top scams retrieved successfully")
                    .data(response)
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Error retrieving all top scams: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }
}