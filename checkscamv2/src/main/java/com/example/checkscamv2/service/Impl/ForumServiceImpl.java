package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.dto.request.CreateForumCommentRequest;
import com.example.checkscamv2.dto.request.CreateForumPostRequest;
import com.example.checkscamv2.dto.response.ForumCommentResponse;
import com.example.checkscamv2.dto.response.ForumPostResponse;
import com.example.checkscamv2.dto.response.UserForumProfileResponse;
import com.example.checkscamv2.entity.ForumComment;
import com.example.checkscamv2.entity.ForumLike;
import com.example.checkscamv2.entity.ForumPost;
import com.example.checkscamv2.entity.User;
import com.example.checkscamv2.constant.RoleName;
import com.example.checkscamv2.exception.DataNotFoundException;
import com.example.checkscamv2.exception.InvalidParamException;
import com.example.checkscamv2.repository.ForumCommentRepository;
import com.example.checkscamv2.repository.ForumLikeRepository;
import com.example.checkscamv2.repository.ForumPostRepository;
import com.example.checkscamv2.repository.ReportRepository;
import com.example.checkscamv2.repository.UserRepository;
import com.example.checkscamv2.service.ForumService;
import com.example.checkscamv2.service.NotificationService;
import com.example.checkscamv2.service.ViewCountService;
import com.example.checkscamv2.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ForumServiceImpl implements ForumService {

    private final ForumPostRepository forumPostRepository;
    private final ForumCommentRepository forumCommentRepository;
    private final ForumLikeRepository forumLikeRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final ViewCountService viewCountService;
    // DISABLED NOTIFICATION SYSTEM
    // private final NotificationService notificationService;

    @Value("${app.upload.forum-images:uploads/forum}")
    private String uploadDir;


    // ✅ Cache user trong request scope để tránh multiple queries
    private static final ThreadLocal<User> currentUserCache = new ThreadLocal<>();
    private static final ThreadLocal<String> currentEmailCache = new ThreadLocal<>();

    // Post operations
    @Override
    @Transactional(readOnly = true)
    public Page<ForumPostResponse> getAllPosts(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<ForumPost> posts = forumPostRepository.findActivePostsOrderByCreatedAtDesc(pageable);
        
        
        return posts.map(post -> convertToPostResponse(post, currentUser));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ForumPostResponse> getPostsByType(String postType, Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<ForumPost> posts = forumPostRepository.findByPostTypeOrderByCreatedAtDesc(postType, pageable);
        return posts.map(post -> convertToPostResponse(post, currentUser));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ForumPostResponse> searchPosts(String keyword, Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<ForumPost> posts = forumPostRepository.searchPosts(keyword, pageable);
        return posts.map(post -> convertToPostResponse(post, currentUser));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ForumPostResponse> getPopularPosts(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<ForumPost> posts = forumPostRepository.findPopularPosts(pageable);
        return posts.map(post -> convertToPostResponse(post, currentUser));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ForumPostResponse> getTrendingPosts(Pageable pageable) {
        User currentUser = getCurrentUser();
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        Page<ForumPost> posts = forumPostRepository.findTrendingPosts(weekAgo, pageable);
        return posts.map(post -> convertToPostResponse(post, currentUser));
    }

    @Override
    @Transactional(readOnly = true)
    public ForumPostResponse getPostById(Long id) {
        User currentUser = getCurrentUser();
        ForumPost post = forumPostRepository.findActiveById(id)
                .orElseThrow(() -> new DataNotFoundException("Post not found with id: " + id));
        
        ForumPostResponse response = convertToPostResponse(post, currentUser);
        
        // Load comments
        List<ForumComment> comments = forumCommentRepository.findByPostOrderByCreatedAtAsc(post);
        response.setComments(comments.stream()
                .map(comment -> convertToCommentResponse(comment, currentUser))
                .collect(Collectors.toList()));
        
        // Increment view count after transaction completes using separate service
        org.springframework.transaction.support.TransactionSynchronizationManager
            .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    viewCountService.incrementPostViewCount(id);
                }
            });
        
        return response;
    }

    @Override
    public ForumPostResponse createPost(CreateForumPostRequest request) {
        User currentUser = getCurrentUserRequired();
        
        ForumPost post = ForumPost.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .postType(request.getPostType() != null ? request.getPostType() : "general")
                .author(currentUser)
                .likesCount(0)
                .commentsCount(0)
                .viewCount(0)
                .isPinned(false)
                .isActive(false) // Posts require admin approval by default
                .build();

        ForumPost savedPost = forumPostRepository.save(post);
        log.info("Created new forum post with id: {} by user: {}", savedPost.getId(), currentUser.getEmail());
        
        return convertToPostResponse(savedPost, currentUser);
    }

    @Override
    public ForumPostResponse updatePost(Long id, CreateForumPostRequest request) {
        User currentUser = getCurrentUserRequired();
        ForumPost post = forumPostRepository.findActiveById(id)
                .orElseThrow(() -> new DataNotFoundException("Post not found with id: " + id));

        // Check if user is the author or admin
        if (!post.getAuthor().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            throw new InvalidParamException("You don't have permission to update this post");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        if (request.getImageUrl() != null) {
            post.setImageUrl(request.getImageUrl());
        }
        if (request.getPostType() != null) {
            post.setPostType(request.getPostType());
        }

        ForumPost updatedPost = forumPostRepository.save(post);
        log.info("Updated forum post with id: {} by user: {}", updatedPost.getId(), currentUser.getEmail());
        
        return convertToPostResponse(updatedPost, currentUser);
    }

    @Override
    public void deletePost(Long id) {
        User currentUser = getCurrentUserRequired();
        ForumPost post = forumPostRepository.findActiveById(id)
                .orElseThrow(() -> new DataNotFoundException("Post not found with id: " + id));

        // Check if user is the author or admin
        if (!post.getAuthor().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            throw new InvalidParamException("You don't have permission to delete this post");
        }

        post.setIsActive(false);
        forumPostRepository.save(post);
        
        // Soft delete all comments
        forumCommentRepository.softDeleteCommentsByPost(id);
        
        log.info("Deleted forum post with id: {} by user: {}", id, currentUser.getEmail());
    }

    // Comment operations
    @Override
    @Transactional(readOnly = true)
    public List<ForumCommentResponse> getCommentsByPost(Long postId) {
        User currentUser = getCurrentUser();
        ForumPost post = forumPostRepository.findActiveById(postId)
                .orElseThrow(() -> new DataNotFoundException("Post not found with id: " + postId));

        List<ForumComment> comments = forumCommentRepository.findByPostOrderByCreatedAtAsc(post);
        return comments.stream()
                .map(comment -> convertToCommentResponse(comment, currentUser))
                .collect(Collectors.toList());
    }

    @Override
    public ForumCommentResponse createComment(CreateForumCommentRequest request) {
        User currentUser = getCurrentUserRequired();
        ForumPost post = forumPostRepository.findActiveById(request.getPostId())
                .orElseThrow(() -> new DataNotFoundException("Post not found with id: " + request.getPostId()));

        ForumComment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = forumCommentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new DataNotFoundException("Parent comment not found"));
        }

        ForumComment comment = ForumComment.builder()
                .content(request.getContent())
                .post(post)
                .author(currentUser)
                .parentComment(parentComment)
                .likesCount(0)
                .isActive(true)
                .isAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false)
                .build();

        ForumComment savedComment = forumCommentRepository.save(comment);
        
        // Update comments count
        forumPostRepository.updateCommentsCount(post.getId());
        
        // Create notification for post author (if not commenting on own post)
        // DISABLED NOTIFICATION SYSTEM
        /*
        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            if (parentComment == null) {
                // New comment on post
                notificationService.createCommentNotification(
                    post.getId(), 
                    post.getAuthor().getId(), 
                    currentUser.getId(),
                    post.getTitle(),
                    savedComment.getContent()
                );
            } else {
                // Reply to comment - notify comment author
                if (!parentComment.getAuthor().getId().equals(currentUser.getId())) {
                    notificationService.createReplyNotification(
                        parentComment.getId(),
                        parentComment.getAuthor().getId(),
                        currentUser.getId(),
                        parentComment.getContent(),
                        savedComment.getContent()
                    );
                }
            }
        }
        
        // Check for mentions in comment content
        checkForMentions(savedComment.getContent(), savedComment, currentUser);
        */
        
        log.info("Created new comment on post {} by user: {}", post.getId(), currentUser.getEmail());
        
        return convertToCommentResponse(savedComment, currentUser);
    }

    @Override
    public void deleteComment(Long id) {
        User currentUser = getCurrentUserRequired();
        ForumComment comment = forumCommentRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Comment not found with id: " + id));

        // Check if user is the author or admin
        if (!comment.getAuthor().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            throw new InvalidParamException("You don't have permission to delete this comment");
        }

        forumCommentRepository.softDeleteComment(id);
        
        // Update comments count
        forumPostRepository.updateCommentsCount(comment.getPost().getId());
        
        log.info("Deleted comment with id: {} by user: {}", id, currentUser.getEmail());
    }

    // Like operations
    @Override
    public void likePost(Long postId) {
        User currentUser = getCurrentUserRequired();
        ForumPost post = forumPostRepository.findActiveById(postId)
                .orElseThrow(() -> new DataNotFoundException("Post not found with id: " + postId));

        // Check if already liked
        if (forumLikeRepository.existsByUserAndPost(currentUser, post)) {
            throw new InvalidParamException("You have already liked this post");
        }

        ForumLike like = ForumLike.builder()
                .user(currentUser)
                .post(post)
                .build();

        forumLikeRepository.save(like);
        forumPostRepository.updateLikesCount(postId);
        
        // Create notification for post author (if not liking own post)
        // DISABLED NOTIFICATION SYSTEM
        /*
        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            notificationService.createLikePostNotification(
                postId,
                post.getAuthor().getId(),
                currentUser.getId(),
                post.getTitle()
            );
        }
        */
        
        log.info("User {} liked post {}", currentUser.getEmail(), postId);
    }

    @Override
    public void unlikePost(Long postId) {
        User currentUser = getCurrentUserRequired();
        ForumPost post = forumPostRepository.findActiveById(postId)
                .orElseThrow(() -> new DataNotFoundException("Post not found with id: " + postId));

        ForumLike like = forumLikeRepository.findByUserAndPost(currentUser, post)
                .orElseThrow(() -> new InvalidParamException("You haven't liked this post"));

        forumLikeRepository.delete(like);
        forumPostRepository.updateLikesCount(postId);
        
        log.info("User {} unliked post {}", currentUser.getEmail(), postId);
    }

    @Override
    public void likeComment(Long commentId) {
        User currentUser = getCurrentUserRequired();
        ForumComment comment = forumCommentRepository.findById(commentId)
                .orElseThrow(() -> new DataNotFoundException("Comment not found with id: " + commentId));

        // Check if already liked
        if (forumLikeRepository.existsByUserAndComment(currentUser, comment)) {
            throw new InvalidParamException("You have already liked this comment");
        }

        ForumLike like = ForumLike.builder()
                .user(currentUser)
                .comment(comment)
                .build();

        forumLikeRepository.save(like);
        forumCommentRepository.updateLikesCount(commentId);
        
        // Create notification for comment author (if not liking own comment)
        // DISABLED NOTIFICATION SYSTEM
        /*
        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            notificationService.createLikeCommentNotification(
                commentId,
                comment.getAuthor().getId(),
                currentUser.getId(),
                comment.getContent()
            );
        }
        */
        
        log.info("User {} liked comment {}", currentUser.getEmail(), commentId);
    }

    @Override
    public void unlikeComment(Long commentId) {
        User currentUser = getCurrentUserRequired();
        ForumComment comment = forumCommentRepository.findById(commentId)
                .orElseThrow(() -> new DataNotFoundException("Comment not found with id: " + commentId));

        ForumLike like = forumLikeRepository.findByUserAndComment(currentUser, comment)
                .orElseThrow(() -> new InvalidParamException("You haven't liked this comment"));

        forumLikeRepository.delete(like);
        forumCommentRepository.updateLikesCount(commentId);
        
        log.info("User {} unliked comment {}", currentUser.getEmail(), commentId);
    }

    // User profile
    @Override
    @Transactional(readOnly = true)
    public UserForumProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));

        Long postsCount = forumPostRepository.countByAuthor(user);
        Long reportsCount = reportRepository.countByUser(user.getEmail());

        return UserForumProfileResponse.builder()
                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .userAvatarUrl(user.getAvatar()) // Use avatar field directly
                .joinedDate(user.getCreatedAt())
                .postsCount(postsCount)
                .reportsCount(reportsCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ForumPostResponse> getUserPosts(Long userId, Pageable pageable) {
        User currentUser = getCurrentUser();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));

        Page<ForumPost> posts = forumPostRepository.findByAuthorOrderByCreatedAtDesc(user, pageable);
        return posts.map(post -> convertToPostResponse(post, currentUser));
    }

    // File upload
    // ✅ OPTIMIZED uploadImage - không cần query user vì chỉ upload file
    @Override
    public String uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidParamException("File is empty");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new InvalidParamException("Only image files are allowed");
        }

        // Validate file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new InvalidParamException("File size must not exceed 5MB");
        }

        try {
            // Create upload directory if not exists
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + fileExtension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            // Return URL
            String imageUrl =  "/" + uploadDir + "/" + filename;
            log.info("Uploaded forum image: {} - No user query needed", imageUrl);
            
            return imageUrl;

        } catch (IOException e) {
            log.error("Failed to upload file", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    // Statistics
    @Override
    @Transactional(readOnly = true)
    public Long getTotalPosts() {
        return forumPostRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalActiveUsers() {
        return userRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getPostsCountByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));
        return forumPostRepository.countByAuthor(user);
    }

    // Post approval operations
    @Override
    @Transactional(readOnly = true)
    public Page<ForumPostResponse> getPendingPosts(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<ForumPost> posts = forumPostRepository.findPendingPosts(pageable);
        return posts.map(post -> convertToPostResponse(post, currentUser));
    }

    @Override
    public ForumPostResponse approvePost(Long id) {
        User currentUser = getCurrentUserRequired();
        
        // Only admins can approve posts
        if (!isAdmin(currentUser)) {
            throw new InvalidParamException("You don't have permission to approve posts");
        }
        
        ForumPost post = forumPostRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Post not found with id: " + id));
        
        if (post.getIsActive()) {
            throw new InvalidParamException("Post is already approved");
        }
        
        post.setIsActive(true);
        ForumPost approvedPost = forumPostRepository.save(post);
        
        log.info("Post {} approved by admin {}", id, currentUser.getEmail());
        return convertToPostResponse(approvedPost, currentUser);
    }

    @Override
    public void rejectPost(Long id) {
        User currentUser = getCurrentUserRequired();
        
        // Only admins can reject posts
        if (!isAdmin(currentUser)) {
            throw new InvalidParamException("You don't have permission to reject posts");
        }
        
        ForumPost post = forumPostRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Post not found with id: " + id));
        
        if (post.getIsActive()) {
            throw new InvalidParamException("Cannot reject an already approved post");
        }
        
        // Hard delete rejected posts
        forumPostRepository.delete(post);
        
        log.info("Post {} rejected and deleted by admin {}", id, currentUser.getEmail());
    }

    // ✅ OPTIMIZED Helper methods với caching
    private User getCurrentUser() {
        // Kiểm tra cache trước
        User cachedUser = currentUserCache.get();
        String currentEmail = SecurityUtil.getCurrentUserLogin().orElse(null);
        
        // Nếu có cache và email không đổi, return cache
        if (cachedUser != null && currentEmail != null && 
            currentEmail.equals(currentEmailCache.get())) {
            return cachedUser;
        }
        
        // Clear cache nếu email thay đổi hoặc null
        if (currentEmail == null) {
            currentUserCache.remove();
            currentEmailCache.remove();
            return null;
        }
        
        // Query database và cache kết quả
        User user = userRepository.findByEmail(currentEmail).orElse(null);
        currentUserCache.set(user);
        currentEmailCache.set(currentEmail);
        
        log.debug("Cached user: {} for email: {}", user != null ? user.getName() : "null", currentEmail);
        return user;
    }

    private User getCurrentUserRequired() {
        User user = getCurrentUser();
        if (user == null) {
            throw new InvalidParamException("Authentication required");
        }
        return user;
    }

    // ✅ Method để clear cache khi cần thiết
    private void clearUserCache() {
        currentUserCache.remove();
        currentEmailCache.remove();
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> RoleName.ADMIN.equals(role.getName()));
    }

    private ForumPostResponse convertToPostResponse(ForumPost post, User currentUser) {
        boolean isLiked = false;
        if (currentUser != null) {
            isLiked = forumLikeRepository.existsByUserAndPost(currentUser, post);
        }

        return ForumPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .postType(post.getPostType())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getName())
                .authorEmail(post.getAuthor().getEmail())
                .authorAvatarUrl(post.getAuthor().getAvatar()) // Use avatar field
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .viewCount(post.getViewCount())
                .isLiked(isLiked)
                .isPinned(post.getIsPinned())
                .createdAt(post.getCreatedAt()) // No fallback - use raw value from DB
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private ForumCommentResponse convertToCommentResponse(ForumComment comment, User currentUser) {
        boolean isLiked = false;
        if (currentUser != null) {
            isLiked = forumLikeRepository.existsByUserAndComment(currentUser, comment);
        }

        List<ForumCommentResponse> replies = null;
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            replies = comment.getReplies().stream()
                    .filter(ForumComment::getIsActive)
                    .map(reply -> convertToCommentResponse(reply, currentUser))
                    .collect(Collectors.toList());
        }

        // Handle anonymous comments
        String authorName = comment.getAuthor().getName();
        String authorEmail = comment.getAuthor().getEmail();
        String authorAvatarUrl = comment.getAuthor().getAvatar();
        
        if (comment.getIsAnonymous() != null && comment.getIsAnonymous()) {
            authorName = "Người dùng ẩn danh";
            authorEmail = "anonymous@example.com";
            authorAvatarUrl = null; // No avatar for anonymous comments
        }

        return ForumCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .postId(comment.getPost().getId())
                .authorId(comment.getAuthor().getId())
                .authorName(authorName)
                .authorEmail(authorEmail)
                .authorAvatarUrl(authorAvatarUrl)
                .parentCommentId(comment.getParentComment() != null ? 
                        comment.getParentComment().getId() : null)
                .likesCount(comment.getLikesCount())
                .isLiked(isLiked)
                .isAnonymous(comment.getIsAnonymous())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(replies)
                .build();
    }
    
    // Helper method to check for mentions in content - DISABLED
    /*
    private void checkForMentions(String content, ForumComment comment, User currentUser) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }
        
        // Skip if anonymous comment
        if (Boolean.TRUE.equals(comment.getIsAnonymous())) {
            return;
        }
        
        // Extract mentions using regex
        java.util.regex.Pattern mentionPattern = java.util.regex.Pattern.compile("@([^@\\s]+)");
        java.util.regex.Matcher matcher = mentionPattern.matcher(content);
        
        while (matcher.find()) {
            String mentionedName = matcher.group(1);
            
            // Skip if mentioning self
            if (mentionedName.equals(currentUser.getName())) {
                continue;
            }
            
            // Find user by email (since we only have email as unique identifier)
            Optional<User> mentionedUser = userRepository.findByEmail(mentionedName);
            if (mentionedUser.isEmpty()) {
                // Try finding by name if email not found
                mentionedUser = userRepository.findAll().stream()
                    .filter(user -> mentionedName.equals(user.getName()))
                    .findFirst();
            }
            
            if (mentionedUser.isPresent()) {
                // Create mention notification
                notificationService.createMentionNotification(
                    comment.getPost().getId(),
                    com.example.checkscamv2.entity.Notification.TargetType.COMMENT,
                    mentionedUser.get().getId(),
                    currentUser.getId(),
                    content
                );
                
                log.info("Created mention notification for user {} in comment {}", 
                    mentionedUser.get().getEmail(), comment.getId());
            }
        }
    }
    */
}
