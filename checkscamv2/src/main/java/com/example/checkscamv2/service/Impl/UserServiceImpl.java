package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.constant.RoleName;
import com.example.checkscamv2.dto.ResCreateUserDTO;
import com.example.checkscamv2.dto.request.UpdateUserRequest;
import com.example.checkscamv2.entity.Role;
import com.example.checkscamv2.entity.User;
import com.example.checkscamv2.repository.RoleRepository;
import com.example.checkscamv2.repository.UserRepository;
import com.example.checkscamv2.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public ResCreateUserDTO handleCreateUser(User user, String roleName) {
        ResCreateUserDTO resCreateUserDTO = new ResCreateUserDTO();

        Set<Role> roles = user.getRoles();
        if (roles == null || roles.isEmpty()) {
            Optional<Role> defaultUserRoleOptional = roleRepository.findByName(RoleName.USER);
            if (defaultUserRoleOptional.isEmpty()) {
                throw new RuntimeException("Role 'USER' not found in database. Please ensure it exists.");
            }
            Role defaultUserRole = defaultUserRoleOptional.get();
            roles = new HashSet<>();
            roles.add(defaultUserRole);
            user.setRoles(roles);
        }

        user = userRepository.save(user);
        resCreateUserDTO.setId(user.getId());
        resCreateUserDTO.setEmail(user.getEmail());
        resCreateUserDTO.setName(user.getName());
        return resCreateUserDTO;
    }

    @Override
    public void handleDeleteUser(long id) {
        this.userRepository.deleteById(id);
    }

    @Override
    public User fetchUserById(long id) {
        Optional<User> userOptional = this.userRepository.findById(id);
        return userOptional.orElse(null);
    }

    @Override
    public List<User> fetchAllUser() {
        return this.userRepository.findAll();
    }

    @Override
    public User handleUpdateUser(Long id, UpdateUserRequest updateUserRequest) {
        User currentUser = this.fetchUserById(id);
        if (currentUser != null) {
            if (updateUserRequest != null) {
                Optional.ofNullable(updateUserRequest.getEmail()).ifPresent(currentUser::setEmail);
                Optional.ofNullable(updateUserRequest.getName()).ifPresent(currentUser::setName);

                if (updateUserRequest.getPassword() != null && !updateUserRequest.getPassword().isEmpty()) {
                    String encodedPassword = passwordEncoder.encode(updateUserRequest.getPassword());
                    currentUser.setPassword(encodedPassword);
                }
                MultipartFile avatarFile = updateUserRequest.getAvatar();
                if (avatarFile != null && !avatarFile.isEmpty()) {
                    try {
                        String fileName = System.currentTimeMillis() + "_" + avatarFile.getOriginalFilename();
                        Path uploadPath = Paths.get("uploads");
                        if (!Files.exists(uploadPath)) {
                            Files.createDirectories(uploadPath);
                        }
                        Path filePath = uploadPath.resolve(fileName);
                        Files.copy(avatarFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                        currentUser.setAvatar("uploads/" + fileName);
                    } catch (Exception e) {
                        throw new RuntimeException("Lỗi khi lưu file avatar: " + e.getMessage());
                    }
                }
                Optional.ofNullable(updateUserRequest.getDescription()).ifPresent(currentUser::setDescription);
            }
            currentUser = this.userRepository.save(currentUser);
        }
        return currentUser;
    }

    @Override
    public Optional<User> handleGetUserByUsername(String username) {
        return this.userRepository.findByEmail(username);
    }

    @Override
    public void updateUserToken(String token, String email) {
        Optional<User> currentUserOptional = this.handleGetUserByUsername(email);
        if (currentUserOptional.isPresent()) {
            User currentUser = currentUserOptional.get();
            currentUser.setRefreshToken(token);
            this.userRepository.save(currentUser);
        }
    }
    
    // ✅ Implementation các methods mới
    @Override
    public List<User> fetchUsersByRole(RoleName roleName) {
        return this.userRepository.findUsersByRoleName(roleName);
    }
    
    @Override
    public List<User> fetchCollaborators() {
        return this.userRepository.findCollaborators();
    }
}