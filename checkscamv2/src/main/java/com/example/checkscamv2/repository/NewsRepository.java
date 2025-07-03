package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    
    // Tìm tin chính hiện tại
    Optional<News> findByIsMainNewsTrue();
    
    // Tìm tất cả tin chính
    List<News> findAllByIsMainNewsTrue();
    
    // Tìm tất cả tin thường
    List<News> findAllByIsMainNewsFalse();
    
    // Đếm số tin chính hiện tại
    long countByIsMainNewsTrue();
    
    // Đếm số tin chính (loại trừ ID cụ thể)
    long countByIsMainNewsTrueAndIdNot(Long id);
    
    // Tìm tin chính cũ nhất
    @Query("SELECT n FROM News n WHERE n.isMainNews = true ORDER BY n.createdAt ASC")
    Optional<News> findOldestMainNews();
    
    // Tìm tin chính cũ nhất (loại trừ ID cụ thể)
    @Query("SELECT n FROM News n WHERE n.isMainNews = true AND n.id != :id ORDER BY n.createdAt ASC")
    Optional<News> findOldestMainNewsExcludingId(@Param("id") Long id);
    
    // Đặt tất cả tin về tin thường
    @Modifying
    @Query("UPDATE News n SET n.isMainNews = false WHERE n.isMainNews = true")
    void setAllNewsToRegular();
    
    // Đặt tất cả tin khác về tin thường trừ tin có ID cụ thể
    @Modifying
    @Query("UPDATE News n SET n.isMainNews = false WHERE n.isMainNews = true AND n.id != :excludeId")
    void setAllOtherNewsToRegular(Long excludeId);
}