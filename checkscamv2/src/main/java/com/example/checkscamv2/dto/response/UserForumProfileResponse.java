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
public class UserForumProfileResponse {
    private Long userId;
    private String userName;
    private String userEmail;
    private String userAvatarUrl;
    private LocalDateTime joinedDate;
    private Long postsCount;
    private Long reportsCount;
    private List<ForumPostResponse> recentPosts;
}
