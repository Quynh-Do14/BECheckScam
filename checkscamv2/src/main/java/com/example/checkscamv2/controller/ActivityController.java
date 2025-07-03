package com.example.checkscamv2.controller;

import com.example.checkscamv2.dto.response.ResponseObject;
import com.example.checkscamv2.entity.Activity;
import com.example.checkscamv2.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/activities")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    /**
     * Lấy danh sách activities
     */
    @GetMapping
    public ResponseEntity<ResponseObject> getActivities(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String actionType) {
        
        try {
            List<Activity> activities = activityService.getActivities(limit, offset, actionType);
            return ResponseEntity.ok(
                ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Lấy danh sách hoạt động thành công")
                    .data(activities)
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Lỗi khi lấy danh sách hoạt động: " + e.getMessage())
                    .build()
                );
        }
    }

    /**
     * Lấy thống kê
     */
    @GetMapping("/statistics")
    public ResponseEntity<ResponseObject> getStatistics() {
        try {
            Map<String, Object> stats = activityService.getStatistics();
            return ResponseEntity.ok(
                ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Lấy thống kê thành công")
                    .data(stats)
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Lỗi khi lấy thống kê: " + e.getMessage())
                    .build()
                );
        }
    }

    /**
     * Log đăng tin tức
     */
    @PostMapping("/log/post")
    public ResponseEntity<ResponseObject> logPostActivity(
            @RequestParam Long userId,
            @RequestParam String userName,
            @RequestParam String postTitle,
            @RequestParam(defaultValue = "general") String category) {
        try {
            activityService.logPostActivity(userId, userName, postTitle, category);
            return ResponseEntity.ok(
                ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Đã ghi log hoạt động đăng tin tức")
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Lỗi khi ghi log: " + e.getMessage())
                    .build()
                );
        }
    }

    /**
     * Log gửi báo cáo
     */
    @PostMapping("/log/report")
    public ResponseEntity<ResponseObject> logReportActivity(
            @RequestParam Long userId,
            @RequestParam String userName,
            @RequestParam String reportTitle,
            @RequestParam String reportType) {
        try {
            activityService.logReportActivity(userId, userName, reportTitle, reportType);
            return ResponseEntity.ok(
                ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Đã ghi log hoạt động báo cáo")
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Lỗi khi ghi log: " + e.getMessage())
                    .build()
                );
        }
    }

    /**
     * Log tham gia cộng đồng
     */
    @PostMapping("/log/join")
    public ResponseEntity<ResponseObject> logJoinActivity(
            @RequestParam Long userId,
            @RequestParam String userName,
            @RequestParam String joinedItem) {
        try {
            activityService.logJoinActivity(userId, userName, joinedItem);
            return ResponseEntity.ok(
                ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Đã ghi log hoạt động tham gia")
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Lỗi khi ghi log: " + e.getMessage())
                    .build()
                );
        }
    }
}
