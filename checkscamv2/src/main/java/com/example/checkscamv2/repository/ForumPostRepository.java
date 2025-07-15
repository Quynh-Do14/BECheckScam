package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.ForumPost;
import com.example.checkscamv2.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {
    
    // Find active posts ordered by creation date (newest first)
    @Query("SELECT p FROM ForumPost p WHERE p.isActive = true ORDER BY p.isPinned DESC, p.createdAt DESC")
    Page<ForumPost> findActivePostsOrderByCreatedAtDesc(Pageable pageable);
    
    // Find posts by author
    @Query("SELECT p FROM ForumPost p WHERE p.author = :author AND p.isActive = true ORDER BY p.createdAt DESC")
    Page<ForumPost> findByAuthorOrderByCreatedAtDesc(@Param("author") User author, Pageable pageable);
    
    // Find posts by type
    @Query("SELECT p FROM ForumPost p WHERE p.postType = :postType AND p.isActive = true ORDER BY p.createdAt DESC")
    Page<ForumPost> findByPostTypeOrderByCreatedAtDesc(@Param("postType") String postType, Pageable pageable);
    
    // Search posts by title or content
    @Query("SELECT p FROM ForumPost p WHERE p.isActive = true AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY p.createdAt DESC")
    Page<ForumPost> searchPosts(@Param("keyword") String keyword, Pageable pageable);
    
    // Get popular posts (most liked)
    @Query("SELECT p FROM ForumPost p WHERE p.isActive = true ORDER BY p.likesCount DESC, p.createdAt DESC")
    Page<ForumPost> findPopularPosts(Pageable pageable);
    
    // Count posts by author
    @Query("SELECT COUNT(p) FROM ForumPost p WHERE p.author = :author AND p.isActive = true")
    Long countByAuthor(@Param("author") User author);
    
    // Increment view count
    @Modifying
    @Query("UPDATE ForumPost p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") Long postId);
    
    // Find active post by id
    @Query("SELECT p FROM ForumPost p WHERE p.id = :id AND p.isActive = true")
    Optional<ForumPost> findActiveById(@Param("id") Long id);
    
    // Update likes count
    @Modifying
    @Query("UPDATE ForumPost p SET p.likesCount = " +
           "(SELECT COUNT(l) FROM ForumLike l WHERE l.post = p) WHERE p.id = :postId")
    void updateLikesCount(@Param("postId") Long postId);
    
    // Update comments count
    @Modifying
    @Query("UPDATE ForumPost p SET p.commentsCount = " +
           "(SELECT COUNT(c) FROM ForumComment c WHERE c.post = p AND c.isActive = true) WHERE p.id = :postId")
    void updateCommentsCount(@Param("postId") Long postId);
    
    // Get trending posts (most active in last week)
    @Query("SELECT p FROM ForumPost p WHERE p.isActive = true AND p.createdAt >= :weekAgo " +
           "ORDER BY (p.likesCount + p.commentsCount) DESC, p.createdAt DESC")
    Page<ForumPost> findTrendingPosts(@Param("weekAgo") java.time.LocalDateTime weekAgo, Pageable pageable);
}
