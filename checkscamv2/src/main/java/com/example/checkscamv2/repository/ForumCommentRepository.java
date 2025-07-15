package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.ForumComment;
import com.example.checkscamv2.entity.ForumPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {
    
    // Find comments by post (top-level comments only)
    @Query("SELECT c FROM ForumComment c WHERE c.post = :post AND c.parentComment IS NULL AND c.isActive = true ORDER BY c.createdAt ASC")
    List<ForumComment> findByPostOrderByCreatedAtAsc(@Param("post") ForumPost post);
    
    // Find comments by post with pagination
    @Query("SELECT c FROM ForumComment c WHERE c.post = :post AND c.parentComment IS NULL AND c.isActive = true ORDER BY c.createdAt ASC")
    Page<ForumComment> findByPostOrderByCreatedAtAsc(@Param("post") ForumPost post, Pageable pageable);
    
    // Find replies to a comment
    @Query("SELECT c FROM ForumComment c WHERE c.parentComment = :parentComment AND c.isActive = true ORDER BY c.createdAt ASC")
    List<ForumComment> findRepliesByParentComment(@Param("parentComment") ForumComment parentComment);
    
    // Count active comments by post
    @Query("SELECT COUNT(c) FROM ForumComment c WHERE c.post = :post AND c.isActive = true")
    Long countActiveCommentsByPost(@Param("post") ForumPost post);
    
    // Update likes count for comment
    @Modifying
    @Query("UPDATE ForumComment c SET c.likesCount = " +
           "(SELECT COUNT(l) FROM ForumLike l WHERE l.comment = c) WHERE c.id = :commentId")
    void updateLikesCount(@Param("commentId") Long commentId);
    
    // Find recent comments by user
    @Query("SELECT c FROM ForumComment c WHERE c.author.id = :userId AND c.isActive = true ORDER BY c.createdAt DESC")
    Page<ForumComment> findRecentCommentsByUser(@Param("userId") Long userId, Pageable pageable);
    
    // Delete comment (soft delete)
    @Modifying
    @Query("UPDATE ForumComment c SET c.isActive = false WHERE c.id = :commentId")
    void softDeleteComment(@Param("commentId") Long commentId);
    
    // Delete all comments of a post (soft delete)
    @Modifying
    @Query("UPDATE ForumComment c SET c.isActive = false WHERE c.post.id = :postId")
    void softDeleteCommentsByPost(@Param("postId") Long postId);
}
