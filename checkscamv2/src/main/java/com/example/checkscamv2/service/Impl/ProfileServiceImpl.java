package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.dto.request.CreateProfileRequest;
import com.example.checkscamv2.dto.request.UpdateProfileRequest;
import com.example.checkscamv2.dto.response.ProfileResponse;
import com.example.checkscamv2.entity.Profile;
import com.example.checkscamv2.entity.User;
import com.example.checkscamv2.exception.DataNotFoundException;
import com.example.checkscamv2.repository.ProfileRepository;
import com.example.checkscamv2.repository.UserRepository;
import com.example.checkscamv2.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Override
    public ProfileResponse createProfile(Long userId, CreateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));

        Profile profile = Profile.builder()
                .nameInfo(request.getNameInfo())
                .info(request.getInfo())
                .user(user)
                .build();

        Profile savedProfile = profileRepository.save(profile);
        return mapToProfileResponse(savedProfile);
    }

    @Override
    public ProfileResponse updateProfile(Long profileId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new DataNotFoundException("Profile not found with id: " + profileId));

        profile.setNameInfo(request.getNameInfo());
        profile.setInfo(request.getInfo());

        Profile updatedProfile = profileRepository.save(profile);
        return mapToProfileResponse(updatedProfile);
    }

    @Override
    public ProfileResponse getProfileById(Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new DataNotFoundException("Profile not found with id: " + profileId));
        return mapToProfileResponse(profile);
    }

    @Override
    public List<ProfileResponse> getProfilesByUserId(Long userId) {
        List<Profile> profiles = profileRepository.findByUserId(userId);
        return profiles.stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteProfile(Long profileId) {
        if (!profileRepository.existsById(profileId)) {
            throw new DataNotFoundException("Profile not found with id: " + profileId);
        }
        profileRepository.deleteById(profileId);
    }

    private ProfileResponse mapToProfileResponse(Profile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .nameInfo(profile.getNameInfo())
                .info(profile.getInfo())
                .createdAt(profile.getCreatedAt())
                .build();
    }
} 