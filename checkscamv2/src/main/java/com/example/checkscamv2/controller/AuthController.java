package com.example.checkscamv2.controller;

import com.example.checkscamv2.dto.LoginDTO;
import com.example.checkscamv2.dto.RegisterDTO;
import com.example.checkscamv2.dto.ResCreateUserDTO;
import com.example.checkscamv2.dto.ResLoginDTO;
import com.example.checkscamv2.dto.response.ErrorResponse;
import com.example.checkscamv2.dto.request.ForgotPasswordRequest;
import com.example.checkscamv2.dto.request.ResetPasswordRequest;
import com.example.checkscamv2.entity.User;
import com.example.checkscamv2.exception.IdInvalidException;
import com.example.checkscamv2.service.ActivityService;
import com.example.checkscamv2.service.EmailService;
import com.example.checkscamv2.service.UserService;
import com.example.checkscamv2.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
                .isEmailVerified(false)
                .build();

        String verificationToken = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusSeconds(3600);

        newUser.setEmailVerificationToken(verificationToken);
        newUser.setEmailVerificationTokenExpires(expiryDate);

        ResCreateUserDTO response = userService.handleCreateUser(newUser, null);

        try {
            activityService.logJoinActivity(
                    response.getId(),
                    response.getName(),
                    "Tham gia cộng đồng AI6"
            );
        } catch (Exception e) {
            System.err.println("Failed to log join activity: " + e.getMessage());
        }

        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;
        String emailContent = String.format(
                "Chào mừng bạn đến với AI6, %s!\n\n" +
                        "Vui lòng click vào link sau để xác minh tài khoản của bạn:\n" +
                        "%s\n\n" +
                        "Link này sẽ hết hạn sau 1 giờ. Nếu bạn không yêu cầu đăng ký tài khoản, vui lòng bỏ qua email này.\n\n" +
                        "Trân trọng,\nĐội ngũ AI6",
                newUser.getName(), verificationLink
        );



        try {
            log.info("🚀 Bắt đầu gửi email xác minh...");
            emailService.sendEmail(newUser.getEmail(), "Xác minh tài khoản AI6 của bạn", emailContent);
            log.info("✅ Email xác minh đã gửi thành công!");

        } catch (Exception e) {

        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

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

        userService.setEmailVerified(user);
        return ResponseEntity.status(HttpStatus.OK).body("Email của bạn đã được xác minh thành công!");
    }

    @PostMapping("/resend-verification-email")
    public ResponseEntity<?> resendVerificationEmail(@RequestBody RegisterDTO request) {
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

        String newVerificationToken = UUID.randomUUID().toString();
        Instant newExpiryDate = Instant.now().plusSeconds(3600);

        userService.updateEmailVerificationStatus(user, newVerificationToken, newExpiryDate);

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

            if (currentUserDB.getIsEmailVerified() == null || !currentUserDB.getIsEmailVerified()) {
                throw new RuntimeException("Tài khoản chưa được xác minh email. Vui lòng kiểm tra email của bạn.");
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

    // YÊU CẦU ĐẶT LẠI MẬT KHẨU (GỬI EMAIL)
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        Optional<User> userOptional = userService.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            return ResponseEntity.ok("Nếu tài khoản tồn tại, một email đặt lại mật khẩu đã được gửi.");
        }

        User user = userOptional.get();

        String resetToken = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusSeconds(3600);

        userService.updateResetPasswordToken(user, resetToken, expiryDate);

        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String emailContent = String.format(
                "Xin chào %s,\n\n" +
                        "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản AI6 của mình.\n" +
                        "Vui lòng click vào link sau để đặt lại mật khẩu: \n" +
                        "%s\n\n" +
                        "Link này sẽ hết hạn sau 1 giờ. Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
                        "Trân trọng,\nĐội ngũ AI6",
                user.getName(), resetLink
        );

        try {
            emailService.sendEmail(user.getEmail(), "Đặt lại mật khẩu AI6 của bạn", emailContent);
        } catch (RuntimeException e) {
            System.err.println("Failed to send password reset email to " + user.getEmail() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ErrorResponse("Có lỗi xảy ra khi gửi email đặt lại mật khẩu. Vui lòng thử lại sau.",
                            HttpStatus.INTERNAL_SERVER_ERROR.value(), Instant.now().toEpochMilli())
            );
        }

        return ResponseEntity.ok("Nếu tài khoản tồn tại, một email đặt lại mật khẩu đã được gửi.");
    }

    // ĐẶT LẠI MẬT KHẨU (XÁC THỰC TOKEN VÀ CẬP NHẬT MẬT KHẨU)
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        Optional<User> userOptional = userService.findByResetPasswordToken(request.getToken());

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(
                    "Mã đặt lại mật khẩu không hợp lệ hoặc không tồn tại.",
                    HttpStatus.BAD_REQUEST.value(),
                    Instant.now().toEpochMilli()
            ));
        }

        User user = userOptional.get();

        if (user.getResetPasswordTokenExpires() == null || Instant.now().isAfter(user.getResetPasswordTokenExpires())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(
                    "Mã đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu lại.",
                    HttpStatus.BAD_REQUEST.value(),
                    Instant.now().toEpochMilli()
            ));
        }

        userService.updatePassword(user, request.getNewPassword());

        return ResponseEntity.ok("Mật khẩu của bạn đã được đặt lại thành công.");
    }
}