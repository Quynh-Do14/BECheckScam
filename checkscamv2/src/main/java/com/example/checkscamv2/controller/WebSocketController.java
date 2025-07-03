package com.example.checkscamv2.controller;

import com.example.checkscamv2.entity.Activity;
import com.example.checkscamv2.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

@Controller
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class WebSocketController {
    
    @Autowired
    private ActivityService activityService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * Handle activity messages from frontend
     */
    @MessageMapping("/activity")
    @SendTo("/topic/activities")
    public Activity handleActivity(Activity activity) {
        try {
            // Create activity via service (this will also broadcast)
            return activityService.createActivity(activity);
        } catch (Exception e) {
            System.err.println("Error handling WebSocket activity: " + e.getMessage());
            return activity; // Return original if error
        }
    }
    
    /**
     * Broadcast activity to all connected clients
     */
    public void broadcastActivity(Activity activity) {
        try {
            messagingTemplate.convertAndSend("/topic/activities", activity);
        } catch (Exception e) {
            System.err.println("Failed to broadcast activity: " + e.getMessage());
        }
    }
    
    /**
     * Send statistics update
     */
    public void broadcastStatistics(Object stats) {
        try {
            messagingTemplate.convertAndSend("/topic/stats", stats);
        } catch (Exception e) {
            System.err.println("Failed to broadcast statistics: " + e.getMessage());
        }
    }
}
