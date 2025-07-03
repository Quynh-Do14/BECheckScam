package com.example.checkscamv2.service;

import jakarta.transaction.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UrlScamService {
    @Transactional
    void importUrlData(MultipartFile file) throws IOException;
}
