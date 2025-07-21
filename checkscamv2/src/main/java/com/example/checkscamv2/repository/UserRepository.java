package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.User;
import com.example.checkscamv2.constant.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findTop3ByOrderByCreatedAtDesc();

    List<User> findAllByOrderByIdDesc();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName ORDER BY u.createdAt DESC")
    List<User> findUsersByRoleName(RoleName roleName);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'COLLAB' ORDER BY u.createdAt DESC")
    List<User> findCollaborators();

    Optional<User> findByEmailVerificationToken(String token);

    // PHƯƠNG THỨC ĐỂ TÌM USER BẰNG TOKEN ĐẶT LẠI MẬT KHẨU
    Optional<User> findByResetPasswordToken(String resetPasswordToken);
}