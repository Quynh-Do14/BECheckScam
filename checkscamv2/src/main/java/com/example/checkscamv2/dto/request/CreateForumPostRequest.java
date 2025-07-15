package com.example.checkscamv2.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateForumPostRequest {
    
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;
    
    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;
    
    private String imageUrl;
    
    @Size(max = 50, message = "Post type must not exceed 50 characters")
    private String postType; // news, warning, tip, question
}
