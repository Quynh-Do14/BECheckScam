// src/main/java/com/example/checkscamv2/controller/GoogleAuthController.java
package com.example.checkscamv2.controller;

import com.example.checkscamv2.dto.request.GoogleLoginRequest;
import com.example.checkscamv2.dto.response.JwtResponse;
import com.example.checkscamv2.util.SecurityUtil;
import com.example.checkscamv2.service.UserService;
import com.example.checkscamv2.repository.RoleRepository;
import com.example.checkscamv2.repository.UserRepository;
import com.example.checkscamv2.entity.Role;
import com.example.checkscamv2.entity.User;
import com.example.checkscamv2.constant.RoleName;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class GoogleAuthController {

    @Value("${google.oauth2.client-id}")
    private String googleClientId;

    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    public GoogleAuthController(SecurityUtil securityUtil, UserService userService,
                                RoleRepository roleRepository, UserRepository userRepository,
                                PasswordEncoder passwordEncoder, UserDetailsService userDetailsService) {
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/google") 
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(request.getTokenId());
        } catch (GeneralSecurityException | IOException e) {
            System.out.println(">>> Google ID Token verification failed: " + e.getMessage());
            return ResponseEntity.badRequest().body("Invalid ID Token or Google verification failed.");
        }

        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            Optional<User> userOptional = userService.handleGetUserByUsername(email);
            User user;

            if (userOptional.isPresent()) {
                user = userOptional.get();
                if (name != null && !name.equals(user.getName())) {
                    user.setName(name);
                }
                if (pictureUrl != null && !pictureUrl.equals(user.getAvatar())) {
                    user.setAvatar(pictureUrl);
                }
                userRepository.save(user);
            } else {
                user = User.builder()
                        .email(email)
                        .name(name)
                        .avatar(pictureUrl)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .build();

                Role userRole = roleRepository.findByName(RoleName.USER)
                        .orElseThrow(() -> new RuntimeException("Error: Role USER not found. Please create it in DB."));
                user.getRoles().add(userRole);

                userService.handleCreateUser(user, null);
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = securityUtil.createAccessToken(authentication);

            return ResponseEntity.ok(new JwtResponse(jwt, user.getId(), user.getName(), user.getEmail(), user.getRoles()));

        } else {
            return ResponseEntity.badRequest().body("ID Token is null.");
        }
    }
}