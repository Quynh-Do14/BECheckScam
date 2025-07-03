package com.example.checkscamv2.service;

import com.example.checkscamv2.entity.Activity;

import java.util.List;
import java.util.Map;

public interface ActivityService {
    
    List<Activity> getActivities(int limit, int offset, String actionType);
    
    Activity createActivity(Activity activity);
    
    Map<String, Object> getStatistics();
    
    // Logging methods
    void logPostActivity(Long userId, String userName, String postTitle, String category);
    
    void logReportActivity(Long userId, String userName, String reportTitle, String reportType);
    
    void logJoinActivity(Long userId, String userName, String joinedItem);
}