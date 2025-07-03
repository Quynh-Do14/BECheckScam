package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.component.FileUtils;
import com.example.checkscamv2.constant.ErrorCodeEnum;
import com.example.checkscamv2.dto.request.NewsRequest;
import com.example.checkscamv2.entity.Attachment;
import com.example.checkscamv2.entity.News;
import com.example.checkscamv2.exception.CheckScamException;
import com.example.checkscamv2.exception.FileUploadValidationException;
import com.example.checkscamv2.repository.AttachmentRepository;
import com.example.checkscamv2.repository.NewsRepository;
import com.example.checkscamv2.service.NewsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {
    private final NewsRepository newsRepository;
    private final FileUtils fileUtils;
    private final AttachmentRepository attachmentRepository;
    public static final int MAXIMUM_ATTACHMENTS_PER_REPORT = 5;
    //get all news
    @Override
    public List<News> getAllNews() {
        return newsRepository.findAll();
    }
    
    // get main news - trả về danh sách các tin chính
    @Override
    public List<News> getMainNews() {
        return newsRepository.findAllByIsMainNewsTrue();
    }
    
    // get all regular news
    @Override
    public List<News> getRegularNews() {
        return newsRepository.findAllByIsMainNewsFalse();
    }

    // get news by id
    @Override
    public Optional<News> getNewsById(Long id) {
        return newsRepository.findById(id);
    }

    //post news
    @Override
    @Transactional
    public News createNews(NewsRequest news) {
        News newsEntity = new News();
        newsEntity.setName(news.getName());
        newsEntity.setContent(news.getContent());
        newsEntity.setShortDescription(news.getShortDescription());
        newsEntity.setIsMainNews(news.getIsMainNews() != null ? news.getIsMainNews() : false);
        
        // Nếu đây là tin chính, kiểm tra số lượng tin chính hiện tại
        if (newsEntity.getIsMainNews()) {
            long currentMainNewsCount = newsRepository.countByIsMainNewsTrue();
            if (currentMainNewsCount >= 4) {
                // Tìm tin chính cũ nhất và đặt nó thành tin thường
                Optional<News> oldestMainNews = newsRepository.findOldestMainNews();
                if (oldestMainNews.isPresent()) {
                    News oldest = oldestMainNews.get();
                    oldest.setIsMainNews(false);
                    newsRepository.save(oldest);
                }
            }
        }
        
        return newsRepository.save(newsEntity);
    }

    // put news
    @Override
    @Transactional
    public News updateNews(Long id, NewsRequest news) {
        Optional<News> existingNews = newsRepository.findById(id);
        if (existingNews.isPresent()) {
            News updatedNews = existingNews.get();
            updatedNews.setName(news.getName());
            updatedNews.setShortDescription(news.getShortDescription());
            updatedNews.setContent(news.getContent());
            updatedNews.setIsMainNews(news.getIsMainNews() != null ? news.getIsMainNews() : false);
            updatedNews.setAttachments(existingNews.get().getAttachments());
            
            // Nếu đây là tin chính, kiểm tra số lượng tin chính hiện tại
            if (updatedNews.getIsMainNews()) {
                long currentMainNewsCount = newsRepository.countByIsMainNewsTrueAndIdNot(id);
                if (currentMainNewsCount >= 4) {
                    // Tìm tin chính cũ nhất (không phải tin hiện tại) và đặt nó thành tin thường
                    Optional<News> oldestMainNews = newsRepository.findOldestMainNewsExcludingId(id);
                    if (oldestMainNews.isPresent()) {
                        News oldest = oldestMainNews.get();
                        oldest.setIsMainNews(false);
                        newsRepository.save(oldest);
                    }
                }
            }
            
            return newsRepository.save(updatedNews); // Lưu bản ghi đã cập nhật
        } else {
            return null;
        }
    }

    // DELETE news
    @Override
    @Transactional
    public boolean deleteNews(Long id) {
        Optional<News> newsOptional = newsRepository.findById(id);

        if (newsOptional.isPresent()) {
            News news = newsOptional.get();
            // 1. Xóa các tệp vật lý liên quan trước
            List<Attachment> attachments = news.getAttachments();
            if (attachments != null && !attachments.isEmpty()) {
                for (Attachment attachment : attachments) {
                    String fileUrl = attachment.getUrl();
                    if (fileUrl != null && !fileUrl.isEmpty()) {
                        try {
                            Path filePath = fileUtils.resolve(fileUrl);
                            if (Files.exists(filePath)) {
                                Files.delete(filePath);
                                System.out.println("Đã xóa tệp: " + filePath.getFileName());
                            } else {
                                System.out.println("Tệp không tồn tại: " + filePath.getFileName() + ". Bỏ qua việc xóa.");
                            }
                        } catch (IOException e) {
                            System.err.println("Lỗi khi xóa tệp: " + fileUrl + " - " + e.getMessage());
                        }
                    }
                }
            }
            // 2. Xóa bản ghi tin tức khỏi DB.
            newsRepository.delete(news);
            return true;
        } else {
            return false;
        }
    }

    @Transactional
    public List<Attachment> uploadAndCreateAttachments(Long newsId,
                                                       List<MultipartFile> files) throws Exception {

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new CheckScamException(ErrorCodeEnum.NOT_FOUND));

        List<MultipartFile> validFiles =
                (files == null ? List.<MultipartFile>of() : files).stream()
                        .filter(f -> f != null && !f.isEmpty() && f.getSize() > 0)
                        .toList();

        if (validFiles.isEmpty()) {
            return List.of();
        }


        List<Attachment> saved = new ArrayList<>();

        for (MultipartFile file : validFiles) {
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new FileUploadValidationException(
                        "Kích thước file vượt quá 10MB: " + file.getOriginalFilename(),
                        HttpStatus.PAYLOAD_TOO_LARGE);
            }
            if (file.getContentType() == null
                    || !file.getContentType().startsWith("image/")) {
                throw new FileUploadValidationException(
                        "File phải là hình ảnh: " + file.getOriginalFilename(),
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }

            String storedName = fileUtils.storeFile(file);

            Attachment toSave = Attachment.builder()
                    .news(news)
                    .url(storedName)
                    .build();

            saved.add(attachmentRepository.save(toSave));
        }

        return saved;
    }

    public Resource loadImage(String imageName) throws IOException {
        validateImageName(imageName);

        Path imagePath = fileUtils.resolve(imageName);

        if (!Files.exists(imagePath)) {
            Path fallback = fileUtils.resolve("notfound.jpeg");
            if (Files.exists(fallback)) {
                return new UrlResource(fallback.toUri());
            }
            throw new FileNotFoundException("Image not found: " + imageName);
        }
        return new UrlResource(imagePath.toUri());
    }

    public String getImageMimeType(String imageName) throws IOException {
        Path path = fileUtils.resolve(imageName);
        if (!Files.exists(path)) {
            path = fileUtils.resolve("notfound.jpeg");
        }
        return Files.probeContentType(path);
    }

    private void validateImageName(String imageName) {
        if (imageName.contains("..") || imageName.contains("/") || imageName.contains("\\")) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }
    }

}
