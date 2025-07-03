package com.example.checkscamv2.controller;

import com.example.checkscamv2.dto.LoginDTO;
import com.example.checkscamv2.dto.RegisterDTO;
import com.example.checkscamv2.dto.ResCreateUserDTO;
import com.example.checkscamv2.dto.ResLoginDTO;
import com.example.checkscamv2.dto.response.ErrorResponse;
import com.example.checkscamv2.entity.User;
import com.example.checkscamv2.exception.IdInvalidException;
import com.example.checkscamv2.service.ActivityService;
import com.example.checkscamv2.service.UserService;
import com.example.checkscamv2.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional; // <-- THÊM DÒNG NÀY
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final UserService userService;
    private final SecurityUtil securityUtil;
    private final PasswordEncoder passwordEncoder;
    private final ActivityService activityService;

    @Value("${checkscam.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponse error = new ErrorResponse(
                errorMessage,
                HttpStatus.BAD_REQUEST.value(),
                Instant.now().toEpochMilli()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterDTO registerDto) {
        // Thay đổi để xử lý Optional
        if (userService.handleGetUserByUsername(registerDto.getEmail()).isPresent()) { // <-- THAY ĐỔI Ở ĐÂY
            ErrorResponse error = new ErrorResponse(
                    "Email đã tồn tại. Vui lòng sử dụng email khác.",
                    HttpStatus.BAD_REQUEST.value(),
                    Instant.now().toEpochMilli()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        String encodedPassword = passwordEncoder.encode(registerDto.getPassword());

        User newUser = User.builder()
                .name(registerDto.getName())
                .email(registerDto.getEmail())
                .password(encodedPassword)
                .build();

        ResCreateUserDTO response = userService.handleCreateUser(newUser, null);

        // ✅ THÊM LOGGING: Log hoạt động tham gia cộng đồng
        try {
            activityService.logJoinActivity(
                response.getId(),
                response.getName(),
                "Tham gia cộng đồng CheckScam"
            );
        } catch (Exception e) {
            // Log error nhưng không làm fail registration
            System.err.println("Failed to log join activity: " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody LoginDTO loginDto) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDto.getUsername(), loginDto.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        String access_token = this.securityUtil.createAccessToken(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ResLoginDTO res = new ResLoginDTO();
        // Thay đổi để xử lý Optional
        Optional<User> currentUserDBOptional = this.userService.handleGetUserByUsername(loginDto.getUsername());
        if (currentUserDBOptional.isPresent()) {
            User currentUserDB = currentUserDBOptional.get();
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                    currentUserDB.getId(),
                    currentUserDB.getEmail(),
                    currentUserDB.getName());
            res.setUser(userLogin);
        } else {
            throw new RuntimeException("Người dùng không tìm thấy sau khi xác thực thành công.");
        }
        res.setAccessToken(access_token);

        String refresh_token = this.securityUtil.createRefreshToken(loginDto.getUsername(), res);

        this.userService.updateUserToken(refresh_token, loginDto.getUsername());

        ResponseCookie resCookies = ResponseCookie
                .from("refresh_token", refresh_token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";

        if (email.isEmpty()) {
            throw new IdInvalidException("Access Token không hợp lệ");
        }

        this.userService.updateUserToken(null, email);

        ResponseCookie deleteSpringCookie = ResponseCookie.from("refresh_token", null)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteSpringCookie.toString())
                .body(null);
    }
}