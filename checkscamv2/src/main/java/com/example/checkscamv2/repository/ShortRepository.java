package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.Short;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShortRepository extends JpaRepository<Short, Long> {
} 