package com.example.checkscamv2.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static files từ thư mục uploads
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/", "file:./uploads/", "classpath:/static/uploads/");
        
        // Serve static files từ thư mục hiện tại
        registry.addResourceHandler("/**")
                .addResourceLocations("file:./");
    }
}