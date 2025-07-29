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
            return shortService.getShortById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Update short
    @PutMapping("/{id}")
    public ResponseEntity<Short> updateShort(@PathVariable Long id, @RequestBody Short shortObj) {
        try {
            Short updatedShort = shortService.updateShort(id, shortObj);
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
            shortService.deleteShort(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Increment views
    @PostMapping("/{id}/view")
    public ResponseEntity<Short> incrementViews(@PathVariable Long id) {
        try {
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
} 