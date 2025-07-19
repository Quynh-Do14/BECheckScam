package com.example.checkscamv2.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static files từ thư mục uploads với cache headers
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                    "file:" + System.getProperty("user.dir") + "/uploads/",
                    "file:uploads/",
                    "file:./uploads/",
                    "classpath:/static/uploads/"
                )
                .setCachePeriod(3600) // Cache 1 hour
                .resourceChain(true);
        
        // Serve forum images specifically
        registry.addResourceHandler("/uploads/forum/**")
                .addResourceLocations(
                    "file:" + System.getProperty("user.dir") + "/uploads/forum/",
                    "file:uploads/forum/",
                    "file:./uploads/forum/"
                )
                .setCachePeriod(3600)
                .resourceChain(true);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Cấu hình CORS cho phép nguồn từ Angular dev server
        registry.addMapping("/**")
                .allowedOrigins(
                    "http://localhost:4200",
                    "https://localhost:4200", 
                    "http://127.0.0.1:4200",
                    "https://127.0.0.1:4200"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}