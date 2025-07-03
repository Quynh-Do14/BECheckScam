package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.ResCreateUserDTO;
import com.example.checkscamv2.dto.request.UpdateUserRequest;
import com.example.checkscamv2.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional; // <-- THÊM DÒNG NÀY

public interface UserService {
    ResCreateUserDTO handleCreateUser(User user, String roleName);
    void handleDeleteUser(long id);
    User fetchUserById(long id);
    List<User> fetchAllUser();
    User handleUpdateUser(Long id, UpdateUserRequest updateUserRequest);
    Optional<User> handleGetUserByUsername(String username);
    void updateUserToken(String token, String email);
}