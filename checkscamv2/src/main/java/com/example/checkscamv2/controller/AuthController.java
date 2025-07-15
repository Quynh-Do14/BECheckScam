// File: src/main/java/com/example/checkscamv2/controller/AuthController.java

package com.example.checkscamv2.controller;

import com.example.checkscamv2.dto.LoginDTO;
import com.example.checkscamv2.dto.RegisterDTO;
import com.example.checkscamv2.dto.ResCreateUserDTO;
import com.example.checkscamv2.dto.ResLoginDTO;
import com.example.checkscamv2.dto.response.ErrorResponse;
import com.example.checkscamv2.entity.User;
import com.example.checkscamv2.exception.IdInvalidException;
import com.example.checkscamv2.service.ActivityService;
import com.example.checkscamv2.service.EmailService;
import com.example.checkscamv2.service.UserService;
import com.example.checkscamv2.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.DefaultMessageSourceResolvable;
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
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
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
    private final EmailService emailService;

    @Value("${checkscam.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
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
        if (userService.handleGetUserByUsername(registerDto.getEmail()).isPresent()) {
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
                .isEmailVerified(false) // Mặc định là FALSE
                .build();

        // Tạo token và thời gian hết hạn
        String verificationToken = UUID.randomUUID().toString(); // Hoặc sử dụng SecureRandom cho an toàn hơn
        Instant expiryDate = Instant.now().plusSeconds(3600); // Token hết hạn sau 1 giờ (3600 giây)

        newUser.setEmailVerificationToken(verificationToken);
        newUser.setEmailVerificationTokenExpires(expiryDate);

        ResCreateUserDTO response = userService.handleCreateUser(newUser, null); // Lưu user với token và trạng thái chưa xác minh

        try {
            activityService.logJoinActivity(
                    response.getId(),
                    response.getName(),
                    "Tham gia cộng đồng AI6"
            );
        } catch (Exception e) {
            System.err.println("Failed to log join activity: " + e.getMessage());
        }

        // Gửi email xác minh
        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken; // Hoặc URL API backend nếu frontend redirect
        String emailContent = String.format(
                "Chào mừng bạn đến với AI6, %s!\n\n" +
                        "Vui lòng click vào link sau để xác minh tài khoản của bạn:\n" +
                        "%s\n\n" +
                        "Link này sẽ hết hạn sau 1 giờ. Nếu bạn không yêu cầu đăng ký tài khoản, vui lòng bỏ qua email này.\n\n" +
                        "Trân trọng,\nĐội ngũ AI6",
                newUser.getName(), verificationLink
        );

        try {
            emailService.sendEmail(newUser.getEmail(), "Xác minh tài khoản AI6 của bạn", emailContent);
        } catch (RuntimeException e) {
            System.err.println("Failed to send verification email: " + e.getMessage());
            // Tùy chọn: Xóa người dùng vừa tạo nếu không thể gửi email để tránh tài khoản rác
            // Hoặc đơn giản là log lỗi và để người dùng yêu cầu gửi lại
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ✅ ENDPOINT ĐỂ XÁC MINH EMAIL
    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        Optional<User> userOptional = userService.findUserByEmailVerificationToken(token);

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mã xác minh không hợp lệ hoặc không tồn tại.");
        }

        User user = userOptional.get();

        if (user.getIsEmailVerified() != null && user.getIsEmailVerified()) {
            return ResponseEntity.status(HttpStatus.OK).body("Email đã được xác minh trước đó.");
        }

        if (user.getEmailVerificationTokenExpires() == null || Instant.now().isAfter(user.getEmailVerificationTokenExpires())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mã xác minh đã hết hạn.");
        }

        // Xác minh thành công
        userService.setEmailVerified(user);
        return ResponseEntity.status(HttpStatus.OK).body("Email của bạn đã được xác minh thành công!");
    }

    // ENDPOINT ĐỂ GỬI LẠI EMAIL XÁC MINH
    @PostMapping("/resend-verification-email")
    public ResponseEntity<?> resendVerificationEmail(@RequestBody RegisterDTO request) { // Dùng lại RegisterDTO cho email
        String email = request.getEmail();
        Optional<User> userOptional = userService.handleGetUserByUsername(email);

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                    "Không tìm thấy người dùng với email này.",
                    HttpStatus.NOT_FOUND.value(),
                    Instant.now().toEpochMilli()
            ));
        }

        User user = userOptional.get();

        if (user.getIsEmailVerified() != null && user.getIsEmailVerified()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(
                    "Email này đã được xác minh rồi.",
                    HttpStatus.BAD_REQUEST.value(),
                    Instant.now().toEpochMilli()
            ));
        }

        // Tạo token mới và thời gian hết hạn mới
        String newVerificationToken = UUID.randomUUID().toString();
        Instant newExpiryDate = Instant.now().plusSeconds(3600); // 1 giờ

        userService.updateEmailVerificationStatus(user, newVerificationToken, newExpiryDate); // Cập nhật token và expiry mới

        // Gửi email mới
        String verificationLink = frontendUrl + "/verify-email?token=" + newVerificationToken;
        String emailContent = String.format(
                "Chào %s,\n\n" +
                        "Bạn đã yêu cầu gửi lại email xác minh cho tài khoản AI6 của mình.\n" +
                        "Vui lòng click vào link sau để xác minh tài khoản của bạn: \n" +
                        "%s\n\n" +
                        "Link này sẽ hết hạn sau 1 giờ. Nếu bạn không yêu cầu gửi lại, vui lòng bỏ qua email này.\n\n" +
                        "Trân trọng,\nĐội ngũ AI6",
                user.getName(), verificationLink
        );

        try {
            emailService.sendEmail(user.getEmail(), "Gửi lại email xác minh tài khoản CheckScam", emailContent);
        } catch (RuntimeException e) {
            System.err.println("Failed to send resend verification email: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                    "Có lỗi xảy ra khi gửi lại email xác minh.",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    Instant.now().toEpochMilli()
            ));
        }

        return ResponseEntity.status(HttpStatus.OK).body("Email xác minh mới đã được gửi. Vui lòng kiểm tra hộp thư của bạn.");
    }


    @PostMapping("/login")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody LoginDTO loginDto) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDto.getUsername(), loginDto.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        String access_token = this.securityUtil.createAccessToken(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ResLoginDTO res = new ResLoginDTO();
        Optional<User> currentUserDBOptional = this.userService.handleGetUserByUsername(loginDto.getUsername());
        if (currentUserDBOptional.isPresent()) {
            User currentUserDB = currentUserDBOptional.get();

            // ✅ THÊM LOGIC KIỂM TRA XÁC MINH EMAIL TRƯỚC KHI ĐĂNG NHẬP
            if (currentUserDB.getIsEmailVerified() == null || !currentUserDB.getIsEmailVerified()) {
                throw new RuntimeException("Tài khoản chưa được xác minh email. Vui lòng kiểm tra email của bạn.");
                // Hoặc bạn có thể trả về một lỗi cụ thể hơn và mã trạng thái HTTP 403 Forbidden
                // return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }

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