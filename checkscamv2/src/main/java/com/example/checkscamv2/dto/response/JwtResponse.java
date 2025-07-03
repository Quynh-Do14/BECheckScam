package com.example.checkscamv2.dto.response;

import com.example.checkscamv2.entity.Role;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class JwtResponse {
    private String token; // JWT Access Token cục bộ
    private String type = "Bearer";
    private Long id;
    private String name;
    private String email;
    private List<String> roles; // Danh sách các vai trò dưới dạng chuỗi

    public JwtResponse(String accessToken, Long id, String name, String email, Set<Role> roles) {
        this.token = accessToken;
        this.id = id;
        this.name = name;
        this.email = email;
        this.roles = roles.stream().map(role -> role.getName().name()).collect(Collectors.toList());
    }
}