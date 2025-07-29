package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.request.CreateNotificationRequest;
import com.example.checkscamv2.dto.response.NotificationResponse;
import com.example.checkscamv2.dto.response.NotificationSummaryResponse;
import com.example.checkscamv2.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    
    // Create notification
    NotificationResponse createNotification(CreateNotificationRequest request);
    
    // Get notifications for current user
    Page<NotificationResponse> getNotifications(Pageable pageable);
    
    // Get notification summary (for header bell)
    NotificationSummaryResponse getNotificationSummary();
    
    // Mark notification as read
    NotificationResponse markAsRead(Long notificationId);
    
    // Mark all notifications as read
    void markAllAsRead();
    
    // Delete notification
    void deleteNotification(Long notificationId);
    
    // Helper methods for creating specific notification types
    void createLikePostNotification(Long postId, Long postAuthorId, Long actorId, String postTitle);
    void createLikeCommentNotification(Long commentId, Long commentAuthorId, Long actorId, String commentContent);
    void createCommentNotification(Long postId, Long postAuthorId, Long actorId, String postTitle, String commentContent);
    void createReplyNotification(Long commentId, Long commentAuthorId, Long actorId, String commentContent, String replyContent);
    void createMentionNotification(Long targetId, Notification.TargetType targetType, Long mentionedUserId, Long actorId, String content);
    
    // Admin methods
    Long getTotalNotifications();
    void cleanupOldNotifications();
}