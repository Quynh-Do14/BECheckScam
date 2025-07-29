package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.Notification;
import com.example.checkscamv2.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Find notifications by recipient
    Page<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);
    
    // Find unread notifications by recipient
    Page<Notification> findByRecipientAndIsReadFalseOrderByCreatedAtDesc(User recipient, Pageable pageable);
    
    // Count unread notifications for user
    Long countByRecipientAndIsReadFalse(User recipient);
    
    // Get recent notifications for summary (limit 10)
    @Query("SELECT n FROM Notification n WHERE n.recipient = :recipient ORDER BY n.createdAt DESC")
    List<Notification> findRecentByRecipient(@Param("recipient") User recipient, Pageable pageable);
    
    // Mark all notifications as read for a user
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.updatedAt = :now WHERE n.recipient = :recipient AND n.isRead = false")
    int markAllAsReadByRecipient(@Param("recipient") User recipient, @Param("now") LocalDateTime now);
    
    // Find notification by ID and recipient (for security)
    Optional<Notification> findByIdAndRecipient(Long id, User recipient);
    
    // Delete old read notifications (cleanup task)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoffDate")
    int deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Check if notification already exists (to prevent duplicates)
    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE " +
           "n.recipient = :recipient AND " +
           "n.actor = :actor AND " +
           "n.type = :type AND " +
           "n.targetType = :targetType AND " +
           "n.targetId = :targetId AND " +
           "n.createdAt > :since")
    boolean existsSimilarNotification(
        @Param("recipient") User recipient,
        @Param("actor") User actor,
        @Param("type") Notification.NotificationType type,
        @Param("targetType") Notification.TargetType targetType,
        @Param("targetId") Long targetId,
        @Param("since") LocalDateTime since
    );
    
    // Get notification statistics for admin
    @Query("SELECT n.type, COUNT(n) FROM Notification n GROUP BY n.type")
    List<Object[]> getNotificationStatsByType();
    
    // Get most active users by notifications received
    @Query("SELECT n.recipient, COUNT(n) as count FROM Notification n " +
           "WHERE n.createdAt > :since " +
           "GROUP BY n.recipient " +
           "ORDER BY count DESC")
    List<Object[]> getMostNotifiedUsers(@Param("since") LocalDateTime since, Pageable pageable);
    
    // Find notifications by target (for cleanup when target is deleted)
    @Query("SELECT n FROM Notification n WHERE n.targetType = :targetType AND n.targetId = :targetId")
    List<Notification> findByTarget(@Param("targetType") Notification.TargetType targetType, @Param("targetId") Long targetId);
    
    // Delete notifications by target (when post/comment is deleted)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.targetType = :targetType AND n.targetId = :targetId")
    int deleteByTarget(@Param("targetType") Notification.TargetType targetType, @Param("targetId") Long targetId);
}