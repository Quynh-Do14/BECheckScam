package com.example.checkscamv2.controller;

// WEBSOCKET DISABLED - Comment out all WebSocket related code
/*
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/websocket")
public class WebSocketTestController {
    
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testWebSocket() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "WebSocket endpoint available");
        response.put("endpoint", "/ws");
        response.put("cors", "enabled");
        response.put("credentials", "disabled");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getWebSocketInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("websocket_url", "ws://localhost:8080/ws");
        response.put("sockjs_url", "http://localhost:8080/ws");
        response.put("topics", new String[]{"/topic/activities", "/topic/stats"});
        response.put("app_destinations", new String[]{"/app/activity"});
        return ResponseEntity.ok(response);
    }
}
*/
