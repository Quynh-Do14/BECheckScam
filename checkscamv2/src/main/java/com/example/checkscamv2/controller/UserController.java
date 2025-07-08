package com.example.checkscamv2.controller;

import com.example.checkscamv2.dto.ResCreateUserDTO;
import com.example.checkscamv2.dto.request.CreateProfileRequest;
import com.example.checkscamv2.dto.request.CreateUserRequest;
import com.example.checkscamv2.dto.request.UpdateProfileRequest;
import com.example.checkscamv2.dto.request.UpdateUserRequest;
import com.example.checkscamv2.dto.response.CheckScamResponse;
import com.example.checkscamv2.dto.response.ProfileResponse;
import com.example.checkscamv2.dto.response.UserProfilesResponse;
import com.example.checkscamv2.entity.User;
import com.example.checkscamv2.constant.RoleName;
import com.example.checkscamv2.exception.IdInvalidException;
import com.example.checkscamv2.service.ProfileService;
import com.example.checkscamv2.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final ProfileService profileService;
    private final PasswordEncoder passwordEncoder;


    @PostMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ResCreateUserDTO> createNewUser(@RequestBody CreateUserRequest request) {
        String hashPassword = this.passwordEncoder.encode(request.getPassword());
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(hashPassword)
                .build();
        ResCreateUserDTO response = this.userService.handleCreateUser(user, request.getRoleName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ExceptionHandler(value = IdInvalidException.class)
    public ResponseEntity<String> handleIdException(IdInvalidException idException) {
        return ResponseEntity.badRequest().body(idException.getMessage());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable("id") long id) {
        this.userService.handleDeleteUser(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public CheckScamResponse<User> getUserById(@PathVariable("id") long id) {
        return new CheckScamResponse<>(this.userService.fetchUserById(id));
    }

    @GetMapping()
    public ResponseEntity<List<User>> getAllUser() {
        return ResponseEntity.status(HttpStatus.OK).body(this.userService.fetchAllUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('COLLAB') or #id == authentication.principal.id")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @ModelAttribute UpdateUserRequest updateUserRequest) {
        User updatedUser = this.userService.handleUpdateUser(id, updateUserRequest);
        if (updatedUser != null) {
            return ResponseEntity.ok(updatedUser);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{userId}/profiles")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('COLLAB') or #userId == authentication.principal.id")
    public ResponseEntity<?> createProfile(
            @PathVariable Long userId,
            @RequestBody CreateProfileRequest request) {
        try {
            ProfileResponse response = profileService.createProfile(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e){
            return ResponseEntity.badRequest().body("lỗi khi thêm thông tin: "+e.getMessage());
        }
    }

    @GetMapping("/{userId}/profiles")
    public ResponseEntity<?> getUserProfiles(@PathVariable Long userId) {
        try{
            List<ProfileResponse> profiles = profileService.getProfilesByUserId(userId);
            return ResponseEntity.ok(profiles);
        }catch (Exception e){
            return ResponseEntity.badRequest().body("lỗi khi lấy thông tin: "+e.getMessage());
        }
    }

    @GetMapping("/profiles/{profileId}")
    public ResponseEntity<?> getProfileById(@PathVariable Long profileId) {
        try {
            ProfileResponse profile = profileService.getProfileById(profileId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lấy thông tin: "+e.getMessage());
        }
    }

    @PutMapping("/profiles/{profileId}")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('COLLAB')")
    public ResponseEntity<?> updateProfile(
            @PathVariable Long profileId,
            @RequestBody UpdateProfileRequest request) {
        try{
            ProfileResponse response = profileService.updateProfile(profileId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi cập nhật thông tin: "+e.getMessage());
        }
    }

    @DeleteMapping("/profiles/{profileId}")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('COLLAB')")
    public ResponseEntity<?> deleteProfile(@PathVariable Long profileId) {
        profileService.deleteProfile(profileId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Profile deleted successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/user-profiles")
    public ResponseEntity<?> getFullProfiles(@PathVariable Long id) {
        try {
            User user = userService.fetchUserById(id);
            List<ProfileResponse> profiles = profileService.getProfilesByUserId(id);
            UserProfilesResponse response = UserProfilesResponse.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .avatar(user.getAvatar())
                    .description(user.getDescription())
                    .profiles(profiles)
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lấy thông tin user kèm profile: " + e.getMessage());
        }
    }
    
    // ✅ API mới: Lấy danh sách cộng tác viên
    @GetMapping("/collaborators")
    public ResponseEntity<?> getCollaborators() {
        try {
            List<User> collaborators = userService.fetchCollaborators();
            return ResponseEntity.ok(collaborators);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lấy danh sách cộng tác viên: " + e.getMessage());
        }
    }
    
    // ✅ API tổng quát: Lấy users theo role
    @GetMapping("/by-role/{roleName}")
    public ResponseEntity<?> getUsersByRole(@PathVariable String roleName) {
        try {
            RoleName role = RoleName.valueOf(roleName.toUpperCase());
            List<User> users = userService.fetchUsersByRole(role);
            return ResponseEntity.ok(users);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Role không hợp lệ: " + roleName + ". Các role hợp lệ: USER, ADMIN, COLLAB");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lấy users theo role: " + e.getMessage());
        }
    }
}