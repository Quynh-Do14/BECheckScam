package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.ScamType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScamTypeRepository extends JpaRepository<ScamType, Long> {
}