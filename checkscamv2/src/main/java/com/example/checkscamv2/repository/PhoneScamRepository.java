package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.PhoneScam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhoneScamRepository extends JpaRepository<PhoneScam, Long> {
    Optional<PhoneScam> findByPhoneNumber(String phoneNumber);
}