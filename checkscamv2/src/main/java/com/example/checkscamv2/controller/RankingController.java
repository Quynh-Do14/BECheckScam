package com.example.checkscamv2.controller;

import com.example.checkscamv2.dto.ResponseHelper;
import com.example.checkscamv2.dto.request.ViewCountRequest;
import com.example.checkscamv2.dto.response.ResponseObject;
import com.example.checkscamv2.dto.response.SubjectDetailResponseDTO;
import com.example.checkscamv2.dto.response.TopScamItemResponseDTO;
import com.example.checkscamv2.dto.response.TopScamsAggregateResponseDTO;
import com.example.checkscamv2.service.ScamStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/ranking")
@RequiredArgsConstructor
@Validated
public class RankingController {

    private final ScamStatsService scamStatsService;
    private final ResponseHelper responseHelper;

    // ===== TOP SCAMS ENDPOINTS =====


    @GetMapping("/top-phones")
    public ResponseEntity<ResponseObject> getTopPhones() {
        log.info("Request received: GET /top-phones");

        return responseHelper.handleServiceCall(
                () -> scamStatsService.getTopPhoneScams(),
                "Top phone scams retrieved successfully",
                "Error retrieving top phone scams"
        );
    }


    @GetMapping("/top-banks")
    public ResponseEntity<ResponseObject> getTopBanks() {
        log.info("Request received: GET /top-banks");

        return responseHelper.handleServiceCall(
                () -> scamStatsService.getTopBankScams(),
                "Top bank scams retrieved successfully",
                "Error retrieving top bank scams"
        );
    }


    @GetMapping("/top-urls")
    public ResponseEntity<ResponseObject> getTopUrls() {
        log.info("Request received: GET /top-urls");

        return responseHelper.handleServiceCall(
                () -> scamStatsService.getTopUrlScams(),
                "Top URL scams retrieved successfully",
                "Error retrieving top URL scams"
        );
    }


    @GetMapping("/top-all")
    public ResponseEntity<ResponseObject> getTopAll() {
        log.info("Request received: GET /top-all");

        return responseHelper.handleServiceCall(
                this::buildTopScamsAggregate,
                "All top scams retrieved successfully",
                "Error retrieving all top scams"
        );
    }

    // ===== SUBJECT DETAIL ENDPOINT =====


    @GetMapping("/subject-detail/{info}")
    public ResponseEntity<ResponseObject> getSubjectDetail(
            @PathVariable
            @NotBlank(message = "Subject info cannot be empty")
            String info,

            @RequestParam
            @NotNull(message = "Type parameter is required")
            @Min(value = 1, message = "Type must be between 1 and 3")
            @Max(value = 3, message = "Type must be between 1 and 3")
            Integer type) {

        log.info("Request received: GET /subject-detail/{} with type={}", info, type);

        try {
            SubjectDetailResponseDTO subjectDetail = scamStatsService.getSubjectDetail(info, type);

            if (subjectDetail == null) {
                log.warn("Subject not found: info={}, type={}", info, type);
                return responseHelper.createNotFoundResponse("Subject not found: " + info);
            }

            log.info("Subject detail retrieved successfully for info={}", info);
            return responseHelper.createSuccessResponse(
                    subjectDetail,
                    "Subject detail retrieved successfully"
            );

        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for subject detail: {}", e.getMessage());
            return responseHelper.createBadRequestResponse("Invalid input: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error retrieving subject detail for info={}, type={}",
                    info, type, e);
            return responseHelper.createErrorResponse("Error retrieving subject detail");
        }
    }

    // ===== VIEW COUNT ENDPOINT =====


    @PostMapping("/increment-view")
    public ResponseEntity<ResponseObject> incrementViewCount(
            @Valid @RequestBody ViewCountRequest request) {

        log.info("Request received: POST /increment-view for info={}, type={}",
                request.getInfo(), request.getType());

        try {
            ViewCountHandler.incrementViewCount(scamStatsService, request);

            log.info("View count incremented successfully for info={}", request.getInfo());
            return responseHelper.createSuccessResponse(
                    null,
                    "View count incremented successfully"
            );

        } catch (IllegalArgumentException e) {
            log.warn("Invalid view count request: {}", e.getMessage());
            return responseHelper.createBadRequestResponse("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error incrementing view count for info={}, type={}",
                    request.getInfo(), request.getType(), e);
            return responseHelper.createErrorResponse("Error incrementing view count");
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    private TopScamsAggregateResponseDTO buildTopScamsAggregate() {
        log.debug("Building top scams aggregate response");

        List<TopScamItemResponseDTO> topPhones = scamStatsService.getTopPhoneScams();
        List<TopScamItemResponseDTO> topBanks = scamStatsService.getTopBankScams();
        List<TopScamItemResponseDTO> topUrls = scamStatsService.getTopUrlScams();

        return TopScamsAggregateResponseDTO.builder()
                .phones(topPhones)
                .banks(topBanks)
                .urls(topUrls)
                .totalCount(topPhones.size() + topBanks.size() + topUrls.size())
                .build();
    }

    // ===== INNER CLASSES =====


    private static class ViewCountHandler {

        public static void incrementViewCount(ScamStatsService service, ViewCountRequest request) {
            String info = request.getInfo();
            Integer type = request.getType();

            switch (type) {
                case 1 -> {
                    log.debug("Incrementing phone view count for: {}", info);
                    service.incrementPhoneViewCount(info);
                }
                case 2 -> {
                    log.debug("Incrementing bank view count for: {}", info);
                    service.incrementBankViewCount(info);
                }
                case 3 -> {
                    log.debug("Incrementing URL view count for: {}", info);
                    service.incrementUrlViewCount(info);
                }
                default -> throw new IllegalArgumentException(
                        String.format("Invalid type: %d. Must be 1 (phone), 2 (bank), or 3 (url)", type)
                );
            }
        }
    }
}