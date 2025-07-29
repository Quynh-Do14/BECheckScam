package com.example.checkscamv2.dto.request;

import com.example.checkscamv2.entity.Notification;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateNotificationRequest {
    private Long recipientId;
    private Notification.NotificationType type;
    private String title;
    private String message;
    private String actionUrl;
    private Long actorId;
    private Notification.TargetType targetType;
    private Long targetId;
    private String targetTitle;
    private String targetContent;
}