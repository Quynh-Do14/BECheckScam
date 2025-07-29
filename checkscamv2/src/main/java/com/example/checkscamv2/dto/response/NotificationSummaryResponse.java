package com.example.checkscamv2.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NotificationSummaryResponse {
    private Long totalUnread;
    private List<NotificationResponse> recentNotifications;
}