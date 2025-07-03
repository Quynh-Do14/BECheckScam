package com.example.checkscamv2.repository;

import com.example.checkscamv2.constant.RoleName;
import com.example.checkscamv2.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}