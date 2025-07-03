package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.entity.User;
import com.example.checkscamv2.service.UserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component("userDetailsService")
public class UserDetailsCustomImpl implements UserDetailsService {

    private final UserService userService;

    public UserDetailsCustomImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Gọi phương thức từ interface UserService, xử lý Optional
        User user = this.userService.handleGetUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username/password không hợp lệ")); // <-- THAY ĐỔI DÒNG NÀY

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().toString()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}