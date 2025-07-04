package com.example.checkscamv2.controller;

import com.example.checkscamv2.component.LocalizationUtils;
import com.example.checkscamv2.constant.MessageKeys;
import com.example.checkscamv2.dto.MonthlyReportStatsDTO;
import com.example.checkscamv2.dto.YearlyReportStatsDTO;
import com.example.checkscamv2.dto.request.CreateReportRequest;
import com.example.checkscamv2.dto.response.RankingPageResponseDTO;
import com.example.checkscamv2.dto.response.ReporterRankingResponseDTO;
import com.example.checkscamv2.dto.response.ResponseMessage;
import com.example.checkscamv2.dto.response.ResponseObject;
import com.example.checkscamv2.entity.Activity;
import com.example.checkscamv2.entity.Attachment;
import com.example.checkscamv2.entity.Report;
import com.example.checkscamv2.entity.User;
import com.example.checkscamv2.exception.*;
import com.example.checkscamv2.service.ActivityService;
import com.example.checkscamv2.service.CaptchaService;
import com.example.checkscamv2.service.ReportService;
import com.example.checkscamv2.service.UserService;
import com.example.checkscamv2.constant.ActivityType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/report")
public class ReportController {
    private final ReportService reportService;
    private final CaptchaService captchaService;
    private final LocalizationUtils localizationUtils;
    private final ActivityService activityService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> createReport(
            @Valid @RequestBody CreateReportRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) {
        try {
            boolean valid = captchaService.verify(request.getCaptchaToken());
            if (!valid) {
                throw new InvalidCaptchaException("Captcha verification failed.");
            }
            // Logic tạo report trong service đã được sửa để set processingStatus = 1
            Report createdReport = reportService.createReport(request);
            
            // ✅ THÊM LOGGING: Log hoạt động gửi báo cáo ngay khi tạo
            try {
                String reporterName = "Người dùng"; // Mặc định
                
                // Lấy tên từ authentication nếu có
                if (authentication != null) {
                    String email = authentication.getName();
                    var  user = userService.handleGetUserByUsername(email);
                    if (user.isPresent() && user.get().getName() != null && !user.get().getName().trim().isEmpty()) {
                        reporterName = user.get().getName();
                    }
                }
                
                // ✅ Luôn sử dụng "Báo cáo lừa đảo" làm targetName mặc định
                String reportTargetName = "Báo cáo lừa đảo";
                String categoryName = createdReport.getCategory() != null ? 
                    createdReport.getCategory().getName() : "general";
                
                // Tạo metadata với reportId để có thể navigate sau này
                String metadata = String.format(
                    "{\"category\":\"%s\",\"reportId\":%d,\"timestamp\":\"%s\",\"status\":\"pending\"}", 
                    categoryName, createdReport.getId(), System.currentTimeMillis()
                );
                
                // Tạo activity với custom metadata
                Activity activity = new Activity();
                activity.setUserId(createdReport.getId()); // Dùng report ID làm userId tạm thời
                activity.setUserName(reporterName); // ✅ Sử dụng tên thay vì email
                activity.setActionType(ActivityType.REPORT);
                activity.setTargetType("report");
                activity.setTargetName(reportTargetName); // ✅ Luôn là "Báo cáo lừa đảo"
                activity.setMetadata(metadata);
                
                activityService.createActivity(activity);
            } catch (Exception logException) {
                // Log error nhưng không làm fail report creation
                System.err.println("Failed to log report creation activity: " + logException.getMessage());
            }
            
            return ResponseEntity.ok(createdReport);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi gửi báo cáo: "+e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) throws CheckScamException {
        try {
            Report report = reportService.getReportById(id);
            return ResponseEntity.ok(report);
        }catch (Exception e){
            return ResponseEntity.badRequest().body("Lỗi khi lấy báo cáo "+e.getMessage());
        }
    }


    @GetMapping("/image/{imageName}")
    public ResponseEntity<?> viewImage(@PathVariable String imageName) throws IOException {
        try {
            Resource resource = reportService.loadImage(imageName);
            String mimeType = reportService.getImageMimeType(imageName);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .body(resource);
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseMessage.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message("Ảnh không tồn tại: " + imageName)
                            .build());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseMessage.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Lỗi khi tải ảnh: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping(value = "/uploads/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseObject> uploadAttachments(
            @PathVariable("id") Long reportId,
            @RequestParam("files")List<MultipartFile> files) {
        try {
            List<Attachment> attachments = reportService.uploadFile(reportId, files);
            if (attachments.isEmpty() && (files == null || files.stream().allMatch(f -> f == null || f.isEmpty() || f.getSize() == 0))) {
                return ResponseEntity.ok().body(ResponseObject.builder()
                        .message(localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_ATTACHMENTS_NO_VALID_FILES))
                        .status(HttpStatus.OK)
                        .data(attachments)
                        .build());
            }

            return ResponseEntity.ok().body(ResponseObject.builder()
                    .message(localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_ATTACHMENTS_SUCCESSFULLY))
                    .status(HttpStatus.OK)
                    .data(attachments)
                    .build());

        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseObject.builder()
                    .message(localizationUtils.getLocalizedMessage(e.getMessage()))
                    .status(HttpStatus.NOT_FOUND)
                    .build());
        } catch (InvalidParamException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseObject.builder()
                    .message(localizationUtils.getLocalizedMessage(e.getMessage()))
                    .status(HttpStatus.BAD_REQUEST)
                    .build());
        } catch (FileUploadValidationException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(ResponseObject.builder()
                    .message(localizationUtils.getLocalizedMessage(e.getMessageKey(), e.getArgs()))
                    .status(e.getHttpStatus())
                    .build());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseObject.builder()
                    .message(localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_ATTACHMENTS_FILE_STORAGE_ERROR, e.getMessage()))
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseObject.builder()
                    .message(localizationUtils.getLocalizedMessage(MessageKeys.ERROR_OCCURRED_DEFAULT, e.getMessage()))
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build());
        }
    }

    // === RANKING ENDPOINTS ===

    /**
     * Lấy bảng xếp hạng người báo cáo với phân trang
     * @param page Trang (bắt đầu từ 0)
     * @param size Số lượng phần tử mỗi trang (mặc định 10)
     * @return Danh sách người báo cáo với phân trang
     */
    @GetMapping("/ranking")
    public ResponseEntity<?> getReporterRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            RankingPageResponseDTO ranking = reportService.getReporterRanking(page, size);
            return ResponseEntity.ok(ranking);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lấy bảng xếp hạng: " + e.getMessage());
        }
    }

    /**
     * Lấy top 3 người báo cáo hàng đầu để hiển thị podium
     * @return Danh sách top 3 người báo cáo
     */
    @GetMapping("/ranking/top3")
    public ResponseEntity<?> getTop3Reporters() {
        try {
            List<ReporterRankingResponseDTO> top3 = reportService.getTop3Reporters();
            return ResponseEntity.ok(top3);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lấy top 3: " + e.getMessage());
        }
    }

    /**
     * Lấy thống kê tổng quan cho trang ranking
     * @return Thống kê tổng số người báo cáo, tổng báo cáo được duyệt, tỷ lệ thành công
     */
    @GetMapping("/ranking/stats")
    public ResponseEntity<?> getRankingStats() {
        try {
            Map<String, Object> stats = reportService.getRankingStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lấy thống kê: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
            return ResponseEntity.ok(reportService.getAll(pageable));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lấy danh sách báo cáo: " + e.getMessage());
        }
    }

    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlyStats(
            @RequestParam Integer year
    ) {
        try {
            List<MonthlyReportStatsDTO> stats = reportService.getMonthlyStats(year);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lấy thống kê theo tháng: " + e.getMessage());
        }
    }

    @GetMapping("/yearly")
    public ResponseEntity<?> getYearlyStats() {
        try {
            List<YearlyReportStatsDTO> stats = reportService.getYearlyStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lấy thống kê theo năm: " + e.getMessage());
        }
    }

    @PatchMapping("/approve/{id}")
    public ResponseEntity<?> approveReport(@PathVariable Long id) {
        try {
            Report updatedReport = reportService.approveReport(id);
            return ResponseEntity.ok(updatedReport);
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi xác nhận báo cáo: " + e.getMessage());
        }
    }

    @PatchMapping("/reject/{id}")
    public ResponseEntity<?> rejectReport(@PathVariable Long id) {
        try {
            Report updatedReport = reportService.rejectReport(id);
            return ResponseEntity.ok(updatedReport);    
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi từ chối báo cáo: " + e.getMessage());
        }
    }
}