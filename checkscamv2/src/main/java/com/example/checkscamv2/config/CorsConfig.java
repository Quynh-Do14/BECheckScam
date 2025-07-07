package com.example.checkscamv2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow specific origins - DO NOT use allowedOriginPatterns with credentials=false
//        configuration.setAllowedOrigins(
//                Arrays.asList("http://localhost:3000", "http://localhost:4173", "http://localhost:4200", "http://127.0.0.1:4200"));

        configuration.addAllowedOriginPattern("*");
        
        // Allow all methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // CRITICAL: Disable credentials completely for WebSocket
        configuration.setAllowCredentials(false);

        // Set max age
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply to all endpoints
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
