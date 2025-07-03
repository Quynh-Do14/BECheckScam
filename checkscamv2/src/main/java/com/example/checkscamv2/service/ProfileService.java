package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.request.CreateProfileRequest;
import com.example.checkscamv2.dto.request.UpdateProfileRequest;
import com.example.checkscamv2.dto.response.ProfileResponse;
import com.example.checkscamv2.entity.Profile;

import java.util.List;

public interface ProfileService {
    ProfileResponse createProfile(Long userId, CreateProfileRequest request);
    ProfileResponse updateProfile(Long profileId, UpdateProfileRequest request);
    ProfileResponse getProfileById(Long profileId);
    List<ProfileResponse> getProfilesByUserId(Long userId);
    void deleteProfile(Long profileId);
} 