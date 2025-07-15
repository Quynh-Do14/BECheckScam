package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.constant.ActivityType;
import com.example.checkscamv2.entity.Activity;
import com.example.checkscamv2.repository.ActivityRepository;
import com.example.checkscamv2.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
// WEBSOCKET DISABLED
// import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActivityServiceImpl implements ActivityService {
    
    @Autowired
    private ActivityRepository activityRepository;
    
    // WEBSOCKET DISABLED
    // @Autowired(required = false)
    // private SimpMessagingTemplate messagingTemplate;
    
    @Override
    public List<Activity> getActivities(int limit, int offset, String actionType) {
        Pageable pageRequest = PageRequest.of(
            offset / limit, 
            limit, 
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        
        if (actionType != null && !actionType.equals("all")) {
            try {
                ActivityType action = ActivityType.fromString(actionType);
                return activityRepository.findByActionTypeOrderByCreatedAtDesc(action, pageRequest).getContent();
            } catch (IllegalArgumentException e) {
                // If invalid action type, return empty list
                return List.of();
            }
        }
        
        // Only return activities with valid action types
        return activityRepository.findValidActivitiesOrderByCreatedAtDesc(pageRequest).getContent();
    }
    
    @Override
    public Activity createActivity(Activity activity) {
        System.out.println("=== DEBUG: createActivity called ===");
        System.out.println("Activity details: userId=" + activity.getUserId() + ", actionType=" + activity.getActionType() + ", targetName=" + activity.getTargetName());
        
        if (activity.getUserName() == null && activity.getUserId() != null) {
            activity.setUserName("User " + activity.getUserId());
        }
        if (activity.getUserAvatar() == null && activity.getUserName() != null) {
            activity.setUserAvatar("https://ui-avatars.com/api/?name=" + 
                activity.getUserName().replace(" ", "+") + "&background=e74c3c&color=fff");
        }
        
        System.out.println("Saving activity to database...");
        Activity savedActivity = activityRepository.save(activity);
        System.out.println("Activity saved successfully with ID: " + savedActivity.getId());
        
        // WEBSOCKET DISABLED - Comment out WebSocket broadcasting
        /*
        if (messagingTemplate != null) {
            try {
                System.out.println("Broadcasting via WebSocket...");
                messagingTemplate.convertAndSend("/topic/activities", savedActivity);
                System.out.println("WebSocket broadcast successful");
            } catch (Exception e) {
                System.err.println("Failed to broadcast activity: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("WebSocket messagingTemplate is null - skipping broadcast");
        }
        */
        System.out.println("WebSocket is disabled - skipping broadcast");
        
        return savedActivity;
    }
    
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalPosts", activityRepository.countByActionType(ActivityType.UPLOAD)); // Thảy đổi POST thành UPLOAD
        stats.put("totalReports", activityRepository.countByActionType(ActivityType.REPORT));
        stats.put("totalJoins", activityRepository.countByActionType(ActivityType.JOIN));
        stats.put("totalActivities", 
            activityRepository.countByActionType(ActivityType.UPLOAD) +
            activityRepository.countByActionType(ActivityType.REPORT) +
            activityRepository.countByActionType(ActivityType.JOIN)
        );
        
        return stats;
    }
    
    @Override
    public void logPostActivity(Long userId, String userName, String postTitle, String category) {
        System.out.println("=== DEBUG: logPostActivity called ===");
        System.out.println("userId: " + userId);
        System.out.println("userName: " + userName);
        System.out.println("postTitle: " + postTitle);
        System.out.println("category: " + category);
        
        Activity activity = new Activity();
        activity.setUserId(userId);
        activity.setUserName(userName);
        activity.setActionType(ActivityType.UPLOAD);  // Sử dụng UPLOAD thay vì POST
        activity.setTargetType("news");
        activity.setTargetName(postTitle);
        activity.setMetadata(String.format(
            "{\"category\":\"%s\",\"timestamp\":\"%s\"}", 
            category, System.currentTimeMillis()
        ));
        
        System.out.println("Activity object created, calling createActivity...");
        Activity savedActivity = createActivity(activity);
        System.out.println("Activity saved with ID: " + savedActivity.getId());
    }
    
    @Override
    public void logReportActivity(Long userId, String userName, String reportTitle, String reportType) {
        Activity activity = new Activity();
        activity.setUserId(userId);
        activity.setUserName(userName);
        activity.setActionType(ActivityType.REPORT);
        activity.setTargetType("report");
        activity.setTargetName(reportTitle);
        activity.setMetadata(String.format(
            "{\"report_type\":\"%s\",\"timestamp\":\"%s\"}", 
            reportType, System.currentTimeMillis()
        ));
        
        createActivity(activity);
    }
    
    @Override
    public void logJoinActivity(Long userId, String userName, String joinedItem) {
        Activity activity = new Activity();
        activity.setUserId(userId);
        activity.setUserName(userName);
        activity.setActionType(ActivityType.JOIN);
        activity.setTargetType("community");
        activity.setTargetName(joinedItem);
        activity.setMetadata(String.format(
            "{\"referrer\":\"website\",\"timestamp\":\"%s\"}", 
            System.currentTimeMillis()
        ));
        
        createActivity(activity);
    }
}
