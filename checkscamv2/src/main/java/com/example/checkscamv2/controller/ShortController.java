package com.example.checkscamv2.controller;

import com.example.checkscamv2.entity.Short;
import com.example.checkscamv2.service.ShortService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shorts")
@RequiredArgsConstructor
public class ShortController {
    private final ShortService shortService;

    @Value("${shorts.upload.dir:uploads/videos/}")
    private String uploadDir;

    @PostMapping
    public ResponseEntity<?> createShort(
            @RequestParam("title") String title,
            @RequestParam("thumbnail") MultipartFile thumbnail,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            // Validate title
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Title is required");
            }
            if (title.length() > 255) {
                return ResponseEntity.badRequest().body("Title must be less than 255 characters");
            }

            // Validate video file
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Video file is required");
            }
            if (file.getSize() > 100 * 1024 * 1024) { // 100MB limit
                return ResponseEntity.badRequest().body("Video file size must be less than 100MB");
            }
            if (!isValidVideoFile(file.getOriginalFilename())) {
                return ResponseEntity.badRequest().body("Invalid video file format. Supported: MP4, AVI, MOV, WMV, FLV, WEBM");
            }

            // Validate thumbnail
            if (thumbnail == null || thumbnail.isEmpty()) {
                return ResponseEntity.badRequest().body("Thumbnail is required");
            }
            if (thumbnail.getSize() > 5 * 1024 * 1024) { // 5MB limit
                return ResponseEntity.badRequest().body("Thumbnail size must be less than 5MB");
            }
            if (!isValidImageFile(thumbnail.getOriginalFilename())) {
                return ResponseEntity.badRequest().body("Invalid image format. Supported: JPG, PNG, GIF, WEBP");
            }

            Short createdShort = shortService.createShort(title, file, thumbnail);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdShort);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @GetMapping
    public ResponseEntity<List<Short>> getAllShorts() {
        try {
            List<Short> shorts = shortService.getAllShorts();
            return ResponseEntity.ok(shorts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get short by ID
    @GetMapping("/{id}")
    public ResponseEntity<Short> getShortById(@PathVariable Long id) {
        try {
            // Validate ID
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().build();
            }

            return shortService.getShortById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Update short
    @PutMapping("/{id}")
    public ResponseEntity<?> updateShort(
            @PathVariable Long id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        try {
            // Validate ID
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().build();
            }

            // Validate title if provided
            if (title != null) {
                if (title.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("Title cannot be empty");
                }
                if (title.length() > 255) {
                    return ResponseEntity.badRequest().body("Title must be less than 255 characters");
                }
            }

            // Validate thumbnail if provided
            if (thumbnail != null && !thumbnail.isEmpty()) {
                if (thumbnail.getSize() > 5 * 1024 * 1024) { // 5MB limit
                    return ResponseEntity.badRequest().body("Thumbnail size must be less than 5MB");
                }
                if (!isValidImageFile(thumbnail.getOriginalFilename())) {
                    return ResponseEntity.badRequest().body("Invalid image format. Supported: JPG, PNG, GIF, WEBP");
                }
            }

            Short updatedShort = shortService.updateShort(id, title, thumbnail);
            return ResponseEntity.ok(updatedShort);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Delete short
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShort(@PathVariable Long id) {
        try {
            // Validate ID
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().build();
            }

            shortService.deleteShort(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Increment views
    @PostMapping("/{id}/view")
    public ResponseEntity<Short> incrementViews(@PathVariable Long id) {
        try {
            // Validate ID
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().build();
            }

            Short updatedShort = shortService.incrementViews(id);
            return ResponseEntity.ok(updatedShort);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Stream video file
    @GetMapping("/videos/{filename:.+}")
    public ResponseEntity<Resource> getVideo(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir, filename);
            Resource resource = new FileSystemResource(filePath);
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Set appropriate content type for video files
            String contentType = determineContentType(filename);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Stream thumbnail image
    @GetMapping("/thumbnails/{filename:.+}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String filename) {
        try {
            String imageDir = uploadDir.replace("videos", "images");
            Path filePath = Paths.get(imageDir, filename);
            Resource resource = new FileSystemResource(filePath);
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Set appropriate content type for image files
            String contentType = determineImageContentType(filename);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "mp4" -> "video/mp4";
            case "avi" -> "video/x-msvideo";
            case "mov" -> "video/quicktime";
            case "wmv" -> "video/x-ms-wmv";
            case "flv" -> "video/x-flv";
            case "webm" -> "video/webm";
            default -> "application/octet-stream";
        };
    }

    private String determineImageContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            default -> "image/jpeg";
        };
    }

    // Validation helper methods
    private boolean isValidVideoFile(String filename) {
        if (filename == null) return false;
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "mp4", "avi", "mov", "wmv", "flv", "webm" -> true;
            default -> false;
        };
    }

    private boolean isValidImageFile(String filename) {
        if (filename == null) return false;
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg", "png", "gif", "webp" -> true;
            default -> false;
        };
    }
} 