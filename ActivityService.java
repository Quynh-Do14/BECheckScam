// ActivityService.java
package com.checkscam.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.checkscam.model.Activity;
import com.checkscam.repository.ActivityRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActivityService {
    
    @Autowired
    private ActivityRepository activityRepository;
    
    public List<Activity> getActivities(int limit, int offset, String actionType) {
        PageRequest pageRequest = PageRequest.of(
            offset / limit, 
            limit, 
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        
        if (actionType != null && !actionType.equals("all")) {
            Activity.ActionType action = Activity.ActionType.valueOf(actionType.toUpperCase());
            return activityRepository.findByActionType(action, pageRequest).getContent();
        }
        
        return activityRepository.findAll(pageRequest).getContent();
    }
    
    public Activity createActivity(Activity activity) {
        // Auto-generate user info if not provided
        if (activity.getUserName() == null) {
            activity.setUserName("User " + activity.getUserId());
        }
        if (activity.getUserAvatar() == null) {
            activity.setUserAvatar("https://ui-avatars.com/api/?name=" + 
                activity.getUserName().replace(" ", "+") + "&background=e74c3c&color=fff");
        }
        
        return activityRepository.save(activity);
    }
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalScans", activityRepository.countByActionType(Activity.ActionType.SCAN));
        stats.put("totalChecks", activityRepository.countByActionType(Activity.ActionType.CHECK));
        stats.put("totalReports", activityRepository.countByActionType(Activity.ActionType.UPLOAD));
        stats.put("totalActivities", activityRepository.count());
        
        // Today's activities
        stats.put("todayActivities", activityRepository.countTodayActivities());
        
        return stats;
    }
    
    public List<Activity> getDangerousActivities() {
        // Find activities with high risk metadata
        return activityRepository.findDangerousActivities(PageRequest.of(0, 10));
    }
    
    // Helper method to log activities from other services
    public void logScanActivity(Long userId, String userName, String website, String riskLevel) {
        Activity activity = new Activity();
        activity.setUserId(userId);
        activity.setUserName(userName);
        activity.setActionType(Activity.ActionType.SCAN);
        activity.setTargetType("website");
        activity.setTargetName("Kiểm tra website: " + website);
        activity.setTargetUrl(website);
        activity.setMetadata("{\"risk_level\":\"" + riskLevel + "\",\"scan_duration\":\"2.1s\"}");
        
        createActivity(activity);
    }
    
    public void logCheckActivity(Long userId, String userName, String phone, String result) {
        Activity activity = new Activity();
        activity.setUserId(userId);
        activity.setUserName(userName);
        activity.setActionType(Activity.ActionType.CHECK);
        activity.setTargetType("phone");
        activity.setTargetName("Tra cứu số điện thoại: " + phone);
        activity.setMetadata("{\"result\":\"" + result + "\",\"confidence\":95}");
        
        createActivity(activity);
    }
    
    public void logReportActivity(Long userId, String userName, String reportType, String description) {
        Activity activity = new Activity();
        activity.setUserId(userId);
        activity.setUserName(userName);
        activity.setActionType(Activity.ActionType.UPLOAD);
        activity.setTargetType("report");
        activity.setTargetName("Báo cáo " + reportType + ": " + description);
        activity.setMetadata("{\"category\":\"" + reportType + "\",\"severity\":\"high\"}");
        
        createActivity(activity);
    }
}