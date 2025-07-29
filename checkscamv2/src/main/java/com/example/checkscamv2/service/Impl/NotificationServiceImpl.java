package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.dto.request.CreateNotificationRequest;
import com.example.checkscamv2.dto.response.NotificationResponse;
import com.example.checkscamv2.dto.response.NotificationSummaryResponse;
import com.example.checkscamv2.entity.Notification;
import com.example.checkscamv2.entity.User;
import com.example.checkscamv2.exception.DataNotFoundException;
import com.example.checkscamv2.exception.InvalidParamException;
import com.example.checkscamv2.repository.NotificationRepository;
import com.example.checkscamv2.repository.UserRepository;
import com.example.checkscamv2.service.NotificationService;
import com.example.checkscamv2.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    
    @Override
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        // Get recipient and actor
        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new DataNotFoundException("Recipient not found"));
        
        User actor = userRepository.findById(request.getActorId())
                .orElseThrow(() -> new DataNotFoundException("Actor not found"));
        
        // Don't create notification if actor is same as recipient
        if (recipient.getId().equals(actor.getId())) {
            log.debug("Skipping notification creation: actor and recipient are the same user");
            return null;
        }
        
        // Check for duplicate notifications in the last 5 minutes to prevent spam
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        if (notificationRepository.existsSimilarNotification(
                recipient, actor, request.getType(), request.getTargetType(), 
                request.getTargetId(), fiveMinutesAgo)) {
            log.debug("Skipping duplicate notification creation");
            return null;
        }
        
        // Create notification
        Notification notification = Notification.builder()
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .actionUrl(request.getActionUrl())
                .recipient(recipient)
                .actor(actor)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .targetTitle(request.getTargetTitle())
                .targetContent(request.getTargetContent())
                .isRead(false)
                .build();
        
        Notification saved = notificationRepository.save(notification);
        log.info("Created notification {} for user {}", saved.getId(), recipient.getEmail());
        
        return convertToResponse(saved);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<Notification> notifications = notificationRepository
                .findByRecipientOrderByCreatedAtDesc(currentUser, pageable);
        
        return notifications.map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public NotificationSummaryResponse getNotificationSummary() {
        User currentUser = getCurrentUser();
        log.info("Getting notification summary for user: {} (ID: {})", currentUser.getEmail(), currentUser.getId());
        
        // Get unread count
        Long unreadCount = notificationRepository.countByRecipientAndIsReadFalse(currentUser);
        
        // Get recent notifications (limit 10)
        List<Notification> recentNotifications = notificationRepository
                .findRecentByRecipient(currentUser, PageRequest.of(0, 10));
        
        List<NotificationResponse> recentResponses = recentNotifications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        log.info("Found {} unread notifications and {} recent notifications for user {}", 
                unreadCount, recentResponses.size(), currentUser.getEmail());
        
        return NotificationSummaryResponse.builder()
                .totalUnread(unreadCount)
                .recentNotifications(recentResponses)
                .build();
    }
    
    @Override
    public NotificationResponse markAsRead(Long notificationId) {
        User currentUser = getCurrentUser();
        
        Notification notification = notificationRepository
                .findByIdAndRecipient(notificationId, currentUser)
                .orElseThrow(() -> new DataNotFoundException("Notification not found"));
        
        if (!notification.getIsRead()) {
            notification.markAsRead();
            notification = notificationRepository.save(notification);
            log.info("Marked notification {} as read for user {}", notificationId, currentUser.getEmail());
        }
        
        return convertToResponse(notification);
    }
    
    @Override
    public void markAllAsRead() {
        User currentUser = getCurrentUser();
        
        int updatedCount = notificationRepository.markAllAsReadByRecipient(currentUser, LocalDateTime.now());
        log.info("Marked {} notifications as read for user {}", updatedCount, currentUser.getEmail());
    }
    
    @Override
    public void deleteNotification(Long notificationId) {
        User currentUser = getCurrentUser();
        
        Notification notification = notificationRepository
                .findByIdAndRecipient(notificationId, currentUser)
                .orElseThrow(() -> new DataNotFoundException("Notification not found"));
        
        notificationRepository.delete(notification);
        log.info("Deleted notification {} for user {}", notificationId, currentUser.getEmail());
    }
    
    // Helper methods for creating specific notification types
    @Override
    public void createLikePostNotification(Long postId, Long postAuthorId, Long actorId, String postTitle) {
        log.info("Creating like post notification - postAuthorId: {}, actorId: {}, postId: {}", postAuthorId, actorId, postId);
        
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .recipientId(postAuthorId)
                .actorId(actorId)
                .type(Notification.NotificationType.LIKE_POST)
                .title("Thích bài viết")
                .message("đã thích bài viết của bạn")
                .actionUrl("/forum/posts/" + postId)
                .targetType(Notification.TargetType.POST)
                .targetId(postId)
                .targetTitle(postTitle)
                .build();
        
        createNotification(request);
    }
    
    @Override
    public void createLikeCommentNotification(Long commentId, Long commentAuthorId, Long actorId, String commentContent) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .recipientId(commentAuthorId)
                .actorId(actorId)
                .type(Notification.NotificationType.LIKE_COMMENT)
                .title("Thích bình luận")
                .message("đã thích bình luận của bạn")
                .actionUrl("/forum/posts/" + commentId) // Will need to get post ID from comment
                .targetType(Notification.TargetType.COMMENT)
                .targetId(commentId)
                .targetContent(commentContent.substring(0, Math.min(100, commentContent.length())))
                .build();
        
        createNotification(request);
    }
    
    @Override
    public void createCommentNotification(Long postId, Long postAuthorId, Long actorId, String postTitle, String commentContent) {
        log.info("Creating comment notification - postAuthorId: {}, actorId: {}, postId: {}", postAuthorId, actorId, postId);
        
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .recipientId(postAuthorId)
                .actorId(actorId)
                .type(Notification.NotificationType.COMMENT)
                .title("Bình luận mới")
                .message("đã bình luận về bài viết của bạn")
                .actionUrl("/forum/posts/" + postId)
                .targetType(Notification.TargetType.POST)
                .targetId(postId)
                .targetTitle(postTitle)
                .targetContent(commentContent.substring(0, Math.min(100, commentContent.length())))
                .build();
        
        createNotification(request);
    }
    
    @Override
    public void createReplyNotification(Long commentId, Long commentAuthorId, Long actorId, String commentContent, String replyContent) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .recipientId(commentAuthorId)
                .actorId(actorId)
                .type(Notification.NotificationType.REPLY)
                .title("Phản hồi bình luận")
                .message("đã phản hồi bình luận của bạn")
                .actionUrl("/forum/posts/" + commentId) // Will need to get post ID from comment
                .targetType(Notification.TargetType.COMMENT)
                .targetId(commentId)
                .targetContent(commentContent.substring(0, Math.min(100, commentContent.length())))
                .build();
        
        createNotification(request);
    }
    
    @Override
    public void createMentionNotification(Long targetId, Notification.TargetType targetType, Long mentionedUserId, Long actorId, String content) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .recipientId(mentionedUserId)
                .actorId(actorId)
                .type(Notification.NotificationType.MENTION)
                .title("Nhắc đến bạn")
                .message("đã nhắc đến bạn trong một bình luận")
                .actionUrl("/forum/posts/" + targetId)
                .targetType(targetType)
                .targetId(targetId)
                .targetContent(content.substring(0, Math.min(100, content.length())))
                .build();
        
        createNotification(request);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long getTotalNotifications() {
        return notificationRepository.count();
    }
    
    @Override
    public void cleanupOldNotifications() {
        // Delete read notifications older than 30 days
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        int deletedCount = notificationRepository.deleteOldReadNotifications(cutoffDate);
        log.info("Cleaned up {} old notifications", deletedCount);
    }
    
    // Helper methods
    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidParamException("Authentication required"));
        
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new DataNotFoundException("User not found"));
    }
    
    private NotificationResponse convertToResponse(Notification notification) {
        User actor = notification.getActor();
        
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .actionUrl(notification.getActionUrl())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .actorId(actor.getId())
                .actorName(actor.getName())
                .actorEmail(actor.getEmail())
                .actorAvatar(actor.getAvatar())
                .targetType(notification.getTargetType())
                .targetId(notification.getTargetId())
                .targetTitle(notification.getTargetTitle())
                .targetContent(notification.getTargetContent())
                .build();
    }
}