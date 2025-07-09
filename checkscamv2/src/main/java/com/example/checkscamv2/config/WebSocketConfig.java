package com.example.checkscamv2.config;

// WEBSOCKET DISABLED - Comment out all WebSocket related code
/*
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.websocket.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${spring.websocket.sockjs.heartbeat-time:25000}")
    private long heartbeatTime;

    @Value("${spring.websocket.sockjs.disconnect-delay:5000}")
    private long disconnectDelay;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple memory-based message broker
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        
        // Main WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins)
                .withSockJS()
                .setHeartbeatTime(heartbeatTime)
                .setDisconnectDelay(disconnectDelay)
                .setStreamBytesLimit(512 * 1024)
                .setHttpMessageCacheSize(1000)
                .setClientLibraryUrl(null);   // ← Tắt cookie requirement
        
        // Simple WebSocket endpoint (without SockJS) for debugging
        registry.addEndpoint("/ws-simple")
                .setAllowedOrigins(origins);
    }
}
*/

