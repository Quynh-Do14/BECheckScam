package com.example.checkscamv2.controller;

import com.example.checkscamv2.component.LocalizationUtils;
import com.example.checkscamv2.constant.MessageKeys;
import com.example.checkscamv2.dto.request.NewsRequest;
import com.example.checkscamv2.dto.response.ResponseObject;
import com.example.checkscamv2.entity.Activity;
import com.example.checkscamv2.entity.Attachment;
import com.example.checkscamv2.entity.News;
import com.example.checkscamv2.exception.DataNotFoundException;
import com.example.checkscamv2.exception.FileUploadValidationException;
import com.example.checkscamv2.exception.InvalidParamException;
import com.example.checkscamv2.service.ActivityService;
import com.example.checkscamv2.service.Impl.NewsServiceImpl;
import com.example.checkscamv2.service.UserService;
import com.example.checkscamv2.constant.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {
    private final NewsServiceImpl newsService;
    private final LocalizationUtils localizationUtils;
    private final ActivityService activityService;
    private final UserService userService;

    // GET all news
    @GetMapping
    public ResponseEntity<List<News>> getAllNews() {
        List<News> newsList = newsService.getAllNews();
        return new ResponseEntity<>(newsList, HttpStatus.OK);
    }
    
    // GET main news
    @GetMapping("/main")
    public ResponseEntity<?> getMainNews() {
        List<News> mainNewsList = newsService.getMainNews();
        if (!mainNewsList.isEmpty()) {
            return new ResponseEntity<>(mainNewsList, HttpStatus.OK);
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Không có tin chính nào");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }
    
    // GET all regular news
    @GetMapping("/regular")
    public ResponseEntity<List<News>> getRegularNews() {
        List<News> regularNews = newsService.getRegularNews();
        return new ResponseEntity<>(regularNews, HttpStatus.OK);
    }

    // GET news by id
    @GetMapping("/{id}")
    public ResponseEntity<Optional<News>> getNewsById(@PathVariable Long id) {
        Optional<News> news = newsService.getNewsById(id);
        if (news.isPresent()) {
            return new ResponseEntity<>(news, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // TEST POST endpoint without auth
    @PostMapping("/test")
    public ResponseEntity<String> testCreateNews(@RequestBody NewsRequest createNewsRequestDto) {
        return ResponseEntity.ok("Test endpoint received: " + createNewsRequestDto.getName());
    }
    
    // TEST ActivityService endpoint
    @PostMapping("/test-activity")
    public ResponseEntity<String> testActivity() {
        try {
            System.out.println("=== Testing ActivityService directly ===");
            
            // Test 1: Basic activity creation
            System.out.println("Test 1: Creating basic activity...");
            activityService.logPostActivity(1L, "Test User", "Test News Title", "test");
            
            // Test 2: Check if activities exist
            System.out.println("Test 2: Checking activities...");
            var activities = activityService.getActivities(10, 0, null);
            System.out.println("Found " + activities.size() + " activities");
            
            return ResponseEntity.ok("Activity test completed. Check console logs. Found " + activities.size() + " activities.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // POST news
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<News> createNews(@RequestBody NewsRequest createNewsRequestDto, Authentication authentication) {
        System.out.println("=== DEBUG: Creating news ===");
        System.out.println("News name: " + createNewsRequestDto.getName());
        System.out.println("Authentication: " + (authentication != null ? "Present" : "Null"));
        
        News createdNews = newsService.createNews(createNewsRequestDto);
        System.out.println("News created with ID: " + createdNews.getId());
        
        // ✅ THÊM LOGGING: Log hoạt động tạo tin tức
        try {
            if (authentication != null) {
                String userDisplayName = "Admin"; // Mặc định
                String email = authentication.getName();
                System.out.println("Email from auth: " + email);
                
                // ✅ Lấy tên từ database thay vì sử dụng email
                var user = userService.handleGetUserByUsername(email);
                if (user.isPresent() && user.get().getName() != null && !user.get().getName().trim().isEmpty()) {
                    userDisplayName = user.get().getName();
                }
                System.out.println("Display name: " + userDisplayName);
                
                // Lấy userId từ authentication principal
                Long userId = extractUserId(authentication);
                System.out.println("Extracted user ID: " + userId);
                
                System.out.println("Calling activityService.logPostActivity...");
                
                // Tạo metadata với newsId để có thể navigate sau này
                String metadata = String.format(
                    "{\"category\":\"news\",\"newsId\":%d,\"timestamp\":\"%s\"}", 
                    createdNews.getId(), System.currentTimeMillis()
                );
                
                // Tạo activity với custom metadata
                Activity activity = new Activity();
                activity.setUserId(userId != null ? userId : 0L);
                activity.setUserName(userDisplayName); // ✅ Sử dụng tên thay vì email
                activity.setActionType(ActivityType.UPLOAD);
                activity.setTargetType("news");
                activity.setTargetName(createdNews.getName());
                activity.setMetadata(metadata);
                
                activityService.createActivity(activity);
                System.out.println("Activity logged successfully!");
            } else {
                System.out.println("Authentication is null - cannot log activity");
            }
        } catch (Exception e) {
            // Log error nhưng không làm fail news creation
            System.err.println("Failed to log news creation activity: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new ResponseEntity<>(createdNews, HttpStatus.CREATED);
    }
    
    // Helper method để extract userId từ authentication
    private Long extractUserId(Authentication authentication) {
        try {
            String email = authentication.getName();
            System.out.println("Looking up user by email: " + email);
            var user = userService.handleGetUserByUsername(email);
            System.out.println("Found user: " + (user.isPresent() ? "ID=" + user.get().getId() : "null"));
            return user.isPresent() ? user.get().getId() : null;
        } catch (Exception e) {
            System.err.println("Could not extract user ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // PUT news
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<News> updateNews(@PathVariable Long id, @RequestBody NewsRequest newsRequestDto) {
        News updatedNews = newsService.updateNews(id, newsRequestDto);
        if (updatedNews != null) {
            return new ResponseEntity<>(updatedNews, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // DELETE news
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> deleteNews(@PathVariable Long id) {
        boolean isDeleted = newsService.deleteNews(id);
        Map<String, String> response = new HashMap<>();
        response.put("message","Deleted News successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/uploads/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAttachments(
            @PathVariable("id") Long newsId,
            @RequestParam("files")List<MultipartFile> files) {
        try {
            List<Attachment> attachments = newsService.uploadAndCreateAttachments(newsId, files);
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

    @GetMapping("/image/{imageName}")
    public ResponseEntity<?> viewImage(@PathVariable String imageName) {
        try {
            Resource image = newsService.loadImage(imageName);
            String mimeType = newsService.getImageMimeType(imageName);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType != null ? mimeType : "application/octet-stream"))
                    .body(image);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Tên file không hợp lệ");
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Không thể tải ảnh");
        }
    }
}