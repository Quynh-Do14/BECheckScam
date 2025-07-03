package com.example.checkscamv2.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    @Value("${app.upload-dir:uploads}")
    private String uploadDirRaw;

    private Path getUploadDir() throws IOException {
        Path dir = Paths.get(uploadDirRaw).toAbsolutePath().normalize();
        if (!Files.isWritable(dir)) {
            throw new IOException("Thư mục không có quyền ghi: " + dir);
        }
        Files.createDirectories(dir);
        return dir;
    }

    public String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File rỗng hoặc không tồn tại");
        }
        
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Kích thước file vượt quá 10MB");
        }
        
        String originalName = file.getOriginalFilename();
        if (originalName != null && !originalName.matches("(?i).*\\.(jpg|jpeg|png)$")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh .jpg, .jpeg, .png");
        }

        String sanitizedName = Path.of(originalName).getFileName().toString();
        String newName = UUID.randomUUID().toString() + "_" + sanitizedName;

        Path target = getUploadDir().resolve(newName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Đã lưu file [{}] vào {}", newName, target);
        } catch (IOException e) {
            log.error("Lỗi khi lưu file [{}]: {}", newName, e.getMessage());
            throw e;
        }

        return newName;
    }

    public Path resolve(String fileName) throws IOException {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }
        return getUploadDir().resolve(fileName).normalize();
    }
}