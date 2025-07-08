package com.example.checkscamv2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:4200,http://127.0.0.1:4200}")
    private String allowedOrigins;

    @Value("${CORS_ALLOWED_METHODS:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${CORS_ALLOWED_HEADERS:*}")
    private String allowedHeaders;

    @Value("${CORS_ALLOW_CREDENTIALS:false}")
    private boolean allowCredentials;

    @Value("${CORS_MAX_AGE:3600}")
    private long maxAge;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse allowed origins from environment variable
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        
        // Parse allowed methods from environment variable
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        configuration.setAllowedMethods(methods);

        // Parse allowed headers from environment variable
        if ("*".equals(allowedHeaders.trim())) {
            configuration.setAllowedHeaders(Arrays.asList("*"));
        } else {
            List<String> headers = Arrays.asList(allowedHeaders.split(","));
            configuration.setAllowedHeaders(headers);
        }

        // Set credentials from environment variable
        configuration.setAllowCredentials(allowCredentials);

        // Set max age from environment variable
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply to all endpoints
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
