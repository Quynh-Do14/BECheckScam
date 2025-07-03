package com.example.checkscamv2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class WebSocketSecurityConfig {

    @Bean(name = "webSocketCorsConfigurationSource")
    public CorsConfigurationSource webSocketCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200", 
            "http://localhost:3000", 
            "http://127.0.0.1:4200"
        ));
        
        // Allow all methods
        configuration.setAllowedMethods(Arrays.asList("*"));
        
        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Disable credentials for WebSocket to avoid CORS issues
        configuration.setAllowCredentials(false);
        
        // Set max age
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/ws/**", configuration);
        source.registerCorsConfiguration("/ws-simple/**", configuration);
        source.registerCorsConfiguration("/sockjs-node/**", configuration);
        
        return source;
    }
}
