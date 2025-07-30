package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.entity.Short;
import com.example.checkscamv2.repository.ShortRepository;
import com.example.checkscamv2.service.ShortService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShortServiceImpl implements ShortService {
    private final ShortRepository shortRepository;

    @Value("${shorts.upload.dir:uploads/videos/}")
    private String uploadDir;

    @Override
    public List<Short> getAllShorts() {
        return shortRepository.findAll();
    }

    @Override
    public Optional<Short> getShortById(Long id) {
        return shortRepository.findById(id);
    }

    @Override
    public Short createShort(String title, MultipartFile video, MultipartFile thumbnail) {
        try {
            // Service layer validation
            validateCreateShortRequest(title, video, thumbnail);
            
            String videoUrl = uploadVideo(video.getBytes(), video.getOriginalFilename());
            String thumbnailUrl = saveThumbnail(thumbnail);
            
            Short shortObj = Short.builder()
                    .title(title.trim())
                    .videoUrl(videoUrl)
                    .thumbnail(thumbnailUrl)
                    .views(0L)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            return shortRepository.save(shortObj);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create short: " + e.getMessage(), e);
        }
    }

    @Override
    public Short updateShort(Long id, String title, MultipartFile thumbnail) {
        // Service layer validation
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid short ID");
        }
        
        Short existingShort = shortRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Short not found with id: " + id));
        
        // Chỉ cập nhật title nếu được cung cấp
        if (title != null && !title.trim().isEmpty()) {
            if (title.length() > 255) {
                throw new IllegalArgumentException("Title must be less than 255 characters");
            }
            existingShort.setTitle(title.trim());
        }
        
        // Chỉ cập nhật thumbnail nếu được cung cấp
        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                // Validate thumbnail
                validateThumbnail(thumbnail);
                
                // Xóa thumbnail cũ nếu có
                deleteOldThumbnail(existingShort.getThumbnail());
                
                // Lưu thumbnail mới
                String thumbnailUrl = saveThumbnail(thumbnail);
                existingShort.setThumbnail(thumbnailUrl);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save thumbnail: " + e.getMessage(), e);
            }
        }
        
        existingShort.setUpdatedAt(LocalDateTime.now());
        return shortRepository.save(existingShort);
    }

    @Override
    public void deleteShort(Long id) {
        // Service layer validation
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid short ID");
        }
        
        Short shortObj = shortRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Short not found with id: " + id));
        
        // Xóa thumbnail
        deleteOldThumbnail(shortObj.getThumbnail());
        
        // Xóa video
        deleteOldVideo(shortObj.getVideoUrl());
        
        // Xóa record từ database
        shortRepository.deleteById(id);
    }

    @Override
    public Short incrementViews(Long id) {
        // Service layer validation
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid short ID");
        }
        
        Short shortObj = shortRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Short not found with id: " + id));
        shortObj.setViews(shortObj.getViews() + 1);
        shortObj.setUpdatedAt(LocalDateTime.now());
        return shortRepository.save(shortObj);
    }

    private String uploadVideo(byte[] fileBytes, String originalFilename) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + StringUtils.cleanPath(originalFilename);
        Path uploadPath = Paths.get(uploadDir);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, fileBytes);
        
        return "/uploads/videos/" + fileName;
    }

    private String saveThumbnail(MultipartFile thumbnail) throws IOException {
        String imageDir = uploadDir.replace("videos", "images");
        String fileName = System.currentTimeMillis() + "_" + StringUtils.cleanPath(thumbnail.getOriginalFilename());
        Path uploadPath = Paths.get(imageDir);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, thumbnail.getBytes());
        return "/uploads/images/" + fileName;
    }

    private void deleteOldThumbnail(String thumbnailUrl) {
        if (thumbnailUrl != null && !thumbnailUrl.trim().isEmpty()) {
            try {
                // Lấy tên file từ URL
                String fileName = thumbnailUrl.substring(thumbnailUrl.lastIndexOf("/") + 1);
                String imageDir = uploadDir.replace("videos", "images");
                Path filePath = Paths.get(imageDir, fileName);
                
                // Xóa file nếu tồn tại
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    System.out.println("Deleted old thumbnail: " + fileName);
                }
            } catch (IOException e) {
                // Log lỗi nhưng không throw exception để không ảnh hưởng đến việc cập nhật
                System.err.println("Failed to delete old thumbnail: " + e.getMessage());
            }
        }
    }

    private void deleteOldVideo(String videoUrl) {
        if (videoUrl != null && !videoUrl.trim().isEmpty()) {
            try {
                // Lấy tên file từ URL
                String fileName = videoUrl.substring(videoUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get(uploadDir, fileName);
                
                // Xóa file nếu tồn tại
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    System.out.println("Deleted old video: " + fileName);
                }
            } catch (IOException e) {
                // Log lỗi nhưng không throw exception
                System.err.println("Failed to delete old video: " + e.getMessage());
            }
        }
    }

    // Validation methods
    private void validateCreateShortRequest(String title, MultipartFile video, MultipartFile thumbnail) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (title.length() > 255) {
            throw new IllegalArgumentException("Title must be less than 255 characters");
        }
        if (video == null || video.isEmpty()) {
            throw new IllegalArgumentException("Video file is required");
        }
        if (video.getSize() > 100 * 1024 * 1024) { // 100MB limit
            throw new IllegalArgumentException("Video file size must be less than 100MB");
        }
        if (thumbnail == null || thumbnail.isEmpty()) {
            throw new IllegalArgumentException("Thumbnail is required");
        }
        if (thumbnail.getSize() > 5 * 1024 * 1024) { // 5MB limit
            throw new IllegalArgumentException("Thumbnail size must be less than 5MB");
        }
    }

    private void validateThumbnail(MultipartFile thumbnail) {
        if (thumbnail.getSize() > 5 * 1024 * 1024) { // 5MB limit
            throw new IllegalArgumentException("Thumbnail size must be less than 5MB");
        }
        String filename = thumbnail.getOriginalFilename();
        if (filename == null || !isValidImageFile(filename)) {
            throw new IllegalArgumentException("Invalid image format. Supported: JPG, PNG, GIF, WEBP");
        }
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