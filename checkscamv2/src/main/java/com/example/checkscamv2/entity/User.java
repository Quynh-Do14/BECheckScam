package com.example.checkscamv2.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String refreshToken;

    @Column(nullable = false)
    private String password;

    private String avatar;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Profile> profiles;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @JsonManagedReference
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    
    private Boolean isEmailVerified; // Mặc định sẽ là false khi tạo mới
    private String emailVerificationToken;
    private Instant emailVerificationTokenExpires;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_password_token_expires")
    private Instant resetPasswordTokenExpires;
}