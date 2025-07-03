package com.example.checkscamv2.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfilesResponse {
    private Long id;
    private String name;
    private String email;
    private String avatar;
    private String description;
    private List<ProfileResponse> profiles;
} 