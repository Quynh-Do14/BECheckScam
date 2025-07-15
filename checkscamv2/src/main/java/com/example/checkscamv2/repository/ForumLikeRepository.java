package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.ForumComment;
import com.example.checkscamv2.entity.ForumLike;
import com.example.checkscamv2.entity.ForumPost;
import com.example.checkscamv2.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ForumLikeRepository extends JpaRepository<ForumLike, Long> {
    
    // Check if user liked a post
    @Query("SELECT l FROM ForumLike l WHERE l.user = :user AND l.post = :post")
    Optional<ForumLike> findByUserAndPost(@Param("user") User user, @Param("post") ForumPost post);
    
    // Check if user liked a comment
    @Query("SELECT l FROM ForumLike l WHERE l.user = :user AND l.comment = :comment")
    Optional<ForumLike> findByUserAndComment(@Param("user") User user, @Param("comment") ForumComment comment);
    
    // Count likes for a post
    @Query("SELECT COUNT(l) FROM ForumLike l WHERE l.post = :post")
    Long countByPost(@Param("post") ForumPost post);
    
    // Count likes for a comment
    @Query("SELECT COUNT(l) FROM ForumLike l WHERE l.comment = :comment")
    Long countByComment(@Param("comment") ForumComment comment);
    
    // Delete like by user and post
    void deleteByUserAndPost(User user, ForumPost post);
    
    // Delete like by user and comment
    void deleteByUserAndComment(User user, ForumComment comment);
    
    // Check if user has liked a post (boolean)
    @Query("SELECT COUNT(l) > 0 FROM ForumLike l WHERE l.user = :user AND l.post = :post")
    boolean existsByUserAndPost(@Param("user") User user, @Param("post") ForumPost post);
    
    // Check if user has liked a comment (boolean)
    @Query("SELECT COUNT(l) > 0 FROM ForumLike l WHERE l.user = :user AND l.comment = :comment")
    boolean existsByUserAndComment(@Param("user") User user, @Param("comment") ForumComment comment);
}
