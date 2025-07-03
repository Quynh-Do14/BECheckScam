package com.example.checkscamv2.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsRequest {
    private String name;
    private String shortDescription;
    private String content;
    
    @JsonProperty("isMain") // Map từ "isMain" trong JSON thành "isMainNews" trong Java
    private Boolean isMainNews = false;
    // Removed attachments field to avoid Jackson circular reference
    // Attachments are handled separately via upload endpoint
}
