package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.BankScam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankScamRepository extends JpaRepository<BankScam, Long> {
    Optional<BankScam> findByBankAccount(String normalizedInfo);
}