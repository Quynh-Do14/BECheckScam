package com.example.checkscamv2.controller;

import com.example.checkscamv2.dto.request.CreateForumCommentRequest;
import com.example.checkscamv2.dto.request.CreateForumPostRequest;
import com.example.checkscamv2.dto.response.ForumCommentResponse;
import com.example.checkscamv2.dto.response.ForumPostResponse;
import com.example.checkscamv2.dto.response.ResponseObject;
import com.example.checkscamv2.dto.response.UserForumProfileResponse;
import com.example.checkscamv2.service.ForumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/forum")
@RequiredArgsConstructor
@Slf4j
public class ForumController {

    private final ForumService forumService;

    // Post endpoints
    @GetMapping("/posts")
    public ResponseEntity<ResponseObject> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ForumPostResponse> posts;
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                posts = forumService.searchPosts(keyword, pageable);
            } else if (type != null && !type.trim().isEmpty()) {
                posts = forumService.getPostsByType(type, pageable);
            } else if ("popular".equals(sort)) {
                posts = forumService.getPopularPosts(pageable);
            } else if ("trending".equals(sort)) {
                posts = forumService.getTrendingPosts(pageable);
            } else {
                posts = forumService.getAllPosts(pageable);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", posts.getContent());
            response.put("total", posts.getTotalElements());
            response.put("page", posts.getNumber());
            response.put("size", posts.getSize());
            response.put("totalPages", posts.getTotalPages());
            
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Posts retrieved successfully")
                    .data(response)
                    .build());
                    
        } catch (Exception e) {
            log.error("Error retrieving posts", e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Error retrieving posts: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<ResponseObject> getPostById(@PathVariable Long id) {
        try {
            ForumPostResponse post = forumService.getPostById(id);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Post retrieved successfully")
                    .data(post)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving post with id: {}", id, e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Error retrieving post: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/posts")
    public ResponseEntity<ResponseObject> createPost(@Valid @RequestBody CreateForumPostRequest request) {
        try {
            ForumPostResponse post = forumService.createPost(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(ResponseObject.builder()
                    .status(HttpStatus.CREATED)
                    .message("Post created successfully")
                    .data(post)
                    .build());
        } catch (Exception e) {
            log.error("Error creating post", e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Error creating post: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/posts/{id}")
    public ResponseEntity<ResponseObject> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody CreateForumPostRequest request) {
        try {
            ForumPostResponse post = forumService.updatePost(id, request);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Post updated successfully")
                    .data(post)
                    .build());
        } catch (Exception e) {
            log.error("Error updating post with id: {}", id, e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Error updating post: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ResponseObject> deletePost(@PathVariable Long id) {
        try {
            forumService.deletePost(id);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Post deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error deleting post with id: {}", id, e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Error deleting post: " + e.getMessage())
                    .build());
        }
    }

    // Comment endpoints
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ResponseObject> getCommentsByPost(@PathVariable Long postId) {
        try {
            List<ForumCommentResponse> comments = forumService.getCommentsByPost(postId);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Comments retrieved successfully")
                    .data(comments)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving comments for post: {}", postId, e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Error retrieving comments: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/comments")
    public ResponseEntity<ResponseObject> createComment(@Valid @RequestBody CreateForumCommentRequest request) {
        try {
            ForumCommentResponse comment = forumService.createComment(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(ResponseObject.builder()
                    .status(HttpStatus.CREATED)
                    .message("Comment created successfully")
                    .data(comment)
                    .build());
        } catch (Exception e) {
            log.error("Error creating comment", e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Error creating comment: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<ResponseObject> deleteComment(@PathVariable Long id) {
        try {
            forumService.deleteComment(id);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Comment deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error deleting comment with id: {}", id, e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Error deleting comment: " + e.getMessage())
                    .build());
        }
    }

    // Like endpoints
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<ResponseObject> likePost(@PathVariable Long postId) {
        try {
            forumService.likePost(postId);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Post liked successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error liking post with id: {}", postId, e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Error liking post: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/posts/{postId}/like")
    public ResponseEntity<ResponseObject> unlikePost(@PathVariable Long postId) {
        try {
            forumService.unlikePost(postId);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Post unliked successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error unliking post with id: {}", postId, e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Error unliking post: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<ResponseObject> likeComment(@PathVariable Long commentId) {
        try {
            forumService.likeComment(commentId);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Comment liked successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error liking comment with id: {}", commentId, e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Error liking comment: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/comments/{commentId}/like")
    public ResponseEntity<ResponseObject> unlikeComment(@PathVariable Long commentId) {
        try {
            forumService.unlikeComment(commentId);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Comment unliked successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error unliking comment with id: {}", commentId, e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Error unliking comment: " + e.getMessage())
                    .build());
        }
    }

    // User profile endpoints
    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<ResponseObject> getUserProfile(@PathVariable Long userId) {
        try {
            UserForumProfileResponse profile = forumService.getUserProfile(userId);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("User profile retrieved successfully")
                    .data(profile)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving user profile for id: {}", userId, e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Error retrieving user profile: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<ResponseObject> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ForumPostResponse> posts = forumService.getUserPosts(userId, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", posts.getContent());
            response.put("total", posts.getTotalElements());
            response.put("page", posts.getNumber());
            response.put("size", posts.getSize());
            response.put("totalPages", posts.getTotalPages());
            
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("User posts retrieved successfully")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving posts for user: {}", userId, e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Error retrieving user posts: " + e.getMessage())
                    .build());
        }
    }

    // File upload endpoint
    @PostMapping("/upload")
    public ResponseEntity<ResponseObject> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = forumService.uploadImage(file);
            
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Image uploaded successfully")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error uploading image", e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Error uploading image: " + e.getMessage())
                    .build());
        }
    }

    // Statistics endpoints
    @GetMapping("/statistics")
    public ResponseEntity<ResponseObject> getStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalPosts", forumService.getTotalPosts());
            stats.put("totalActiveUsers", forumService.getTotalActiveUsers());
            
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Statistics retrieved successfully")
                    .data(stats)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving statistics", e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Error retrieving statistics: " + e.getMessage())
                    .build());
        }
    }
}
