package com.example.checkscamv2.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumPostResponse {
    private Long id;
    private String title;
    private String content;
    private String imageUrl;
    private String postType;
    private Long authorId;
    private String authorName;
    private String authorEmail;
    private String authorAvatarUrl;
    private Integer likesCount;
    private Integer commentsCount;
    private Integer viewCount;
    private Boolean isLiked;
    private Boolean isPinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ForumCommentResponse> comments;
}
