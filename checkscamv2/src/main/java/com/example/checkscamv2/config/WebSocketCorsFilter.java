
//package com.example.checkscamv2.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.filter.CorsFilter;
//
//import java.util.Arrays;
//import java.util.Collections;
//
//@Configuration
//public class WebSocketCorsFilter {
//
//    @Bean
//    public CorsFilter corsFilter() {
//        CorsConfiguration config = new CorsConfiguration();
//
//        // Allow specific origins - DO NOT use allowedOriginPatterns with credentials=false
//        config.setAllowedOrigins(Arrays.asList("*"));
//
//        // Allow all methods
//        config.setAllowedMethods(Arrays.asList("*"));
//
//        // Allow all headers
//        config.setAllowedHeaders(Arrays.asList("*"));
//
//        // CRITICAL: Set credentials to false
//        config.setAllowCredentials(false);
//
//        // Set max age
//        config.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//
//        return new CorsFilter(source);
//    }
//}

