package com.example.checkscamv2.dto.response;

import com.example.checkscamv2.entity.Notification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private Notification.NotificationType type;
    private String title;
    private String message;
    private String actionUrl;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Actor info
    private Long actorId;
    private String actorName;
    private String actorEmail;
    private String actorAvatar;
    
    // Target info
    private Notification.TargetType targetType;
    private Long targetId;
    private String targetTitle;
    private String targetContent;
}