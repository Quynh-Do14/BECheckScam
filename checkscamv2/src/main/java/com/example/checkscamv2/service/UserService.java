package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.ResCreateUserDTO;
import com.example.checkscamv2.dto.request.UpdateUserRequest;
import com.example.checkscamv2.entity.User;
import com.example.checkscamv2.constant.RoleName;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserService {
    ResCreateUserDTO handleCreateUser(User user, String roleName);
    void handleDeleteUser(long id);
    User fetchUserById(long id);
    List<User> fetchAllUser();
    User handleUpdateUser(Long id, UpdateUserRequest updateUserRequest);
    Optional<User> handleGetUserByUsername(String username);
    void updateUserToken(String token, String email);

    // ✅ Thêm methods mới cho role-based queries
    List<User> fetchUsersByRole(RoleName roleName);
    List<User> fetchCollaborators();

    // ✅ THÊM METHOD MỚI CHO XÁC MINH EMAIL
    void updateEmailVerificationStatus(User user, String token, Instant expiry);
    Optional<User> findUserByEmailVerificationToken(String token);
    void setEmailVerified(User user);
}