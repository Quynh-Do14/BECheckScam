package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.request.NewsRequest;
import com.example.checkscamv2.entity.News;

import java.util.List;
import java.util.Optional;

public interface NewsService {
    List<News> getAllNews();
    List<News> getMainNews();
    List<News> getRegularNews();
    Optional<News> getNewsById(Long id);
    News createNews(NewsRequest news);
    News updateNews(Long id, NewsRequest news);
    boolean deleteNews(Long id);
}
