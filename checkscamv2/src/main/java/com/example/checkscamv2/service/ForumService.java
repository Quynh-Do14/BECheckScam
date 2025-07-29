package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.request.CreateForumCommentRequest;
import com.example.checkscamv2.dto.request.CreateForumPostRequest;
import com.example.checkscamv2.dto.response.ForumCommentResponse;
import com.example.checkscamv2.dto.response.ForumPostResponse;
import com.example.checkscamv2.dto.response.UserForumProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ForumService {
    
    // Post operations
    Page<ForumPostResponse> getAllPosts(Pageable pageable);
    Page<ForumPostResponse> getPostsByType(String postType, Pageable pageable);
    Page<ForumPostResponse> searchPosts(String keyword, Pageable pageable);
    Page<ForumPostResponse> getPopularPosts(Pageable pageable);
    Page<ForumPostResponse> getTrendingPosts(Pageable pageable);
    ForumPostResponse getPostById(Long id);
    ForumPostResponse createPost(CreateForumPostRequest request);
    ForumPostResponse updatePost(Long id, CreateForumPostRequest request);
    void deletePost(Long id);
    
    // Comment operations
    List<ForumCommentResponse> getCommentsByPost(Long postId);
    ForumCommentResponse createComment(CreateForumCommentRequest request);
    void deleteComment(Long id);
    
    // Like operations
    void likePost(Long postId);
    void unlikePost(Long postId);
    void likeComment(Long commentId);
    void unlikeComment(Long commentId);
    
    // User profile
    UserForumProfileResponse getUserProfile(Long userId);
    Page<ForumPostResponse> getUserPosts(Long userId, Pageable pageable);
    
    // File upload
    String uploadImage(MultipartFile file);
    
    // Statistics
    Long getTotalPosts();
    Long getTotalActiveUsers();
    Long getPostsCountByUser(Long userId);
    
    // Post approval operations
    Page<ForumPostResponse> getPendingPosts(Pageable pageable);
    ForumPostResponse approvePost(Long id);
    void rejectPost(Long id);
}
