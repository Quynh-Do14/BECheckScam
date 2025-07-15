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
public class ForumCommentResponse {
    private Long id;
    private String content;
    private Long postId;
    private Long authorId;
    private String authorName;
    private String authorEmail;
    private String authorAvatarUrl;
    private Long parentCommentId;
    private Integer likesCount;
    private Boolean isLiked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ForumCommentResponse> replies;
}
