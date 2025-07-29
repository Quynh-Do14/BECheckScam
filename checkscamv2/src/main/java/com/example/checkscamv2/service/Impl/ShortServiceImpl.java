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
            String videoUrl = uploadVideo(video.getBytes(), video.getOriginalFilename());
            String thumbnailUrl = saveThumbnail(thumbnail);
            
            Short shortObj = Short.builder()
                    .title(title)
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
    public Short updateShort(Long id, Short shortObj) {
        Short existingShort = shortRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Short not found with id: " + id));
        
        existingShort.setTitle(shortObj.getTitle());
        existingShort.setThumbnail(shortObj.getThumbnail());
        existingShort.setVideoUrl(shortObj.getVideoUrl());
        existingShort.setUpdatedAt(LocalDateTime.now());
        
        return shortRepository.save(existingShort);
    }

    @Override
    public void deleteShort(Long id) {
        shortRepository.deleteById(id);
    }

    @Override
    public Short incrementViews(Long id) {
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
} 