package com.example.checkscamv2.service;

import com.example.checkscamv2.entity.Short;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;

public interface ShortService {
    List<Short> getAllShorts();
    Optional<Short> getShortById(Long id);
    Short updateShort(Long id, String title, MultipartFile thumbnail);
    void deleteShort(Long id);
    Short incrementViews(Long id);
    Short createShort(String title, MultipartFile video, MultipartFile thumbnail);
} 