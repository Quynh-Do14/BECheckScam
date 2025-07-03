// ActivityController.java
package com.checkscam.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import com.checkscam.model.Activity;
import com.checkscam.service.ActivityService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/activities")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public ResponseEntity<List<Activity>> getActivities(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String actionType) {
        
        List<Activity> activities = activityService.getActivities(limit, offset, actionType);
        return ResponseEntity.ok(activities);
    }

    @PostMapping
    public ResponseEntity<Activity> createActivity(@RequestBody Activity activity) {
        Activity savedActivity = activityService.createActivity(activity);
        
        // Broadcast real-time update
        messagingTemplate.convertAndSend("/topic/activities", savedActivity);
        
        return ResponseEntity.ok(savedActivity);
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        return ResponseEntity.ok(activityService.getStatistics());
    }

    @GetMapping("/dangerous")
    public ResponseEntity<List<Activity>> getDangerousActivities() {
        List<Activity> dangerous = activityService.getDangerousActivities();
        return ResponseEntity.ok(dangerous);
    }

    // WebSocket endpoints
    @MessageMapping("/activity.create")
    @SendTo("/topic/activities")
    public Activity createActivityWebSocket(Activity activity) {
        return activityService.createActivity(activity);
    }
}