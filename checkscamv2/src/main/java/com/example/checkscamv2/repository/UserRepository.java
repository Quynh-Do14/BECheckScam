package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // <-- THÊM DÒNG NÀY

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findTop3ByOrderByCreatedAtDesc();

    List<User> findAllByOrderByIdDesc();
}