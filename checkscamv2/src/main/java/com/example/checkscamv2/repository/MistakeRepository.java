package com.example.checkscamv2.repository;

import com.example.checkscamv2.constant.MistakeStatus;
import com.example.checkscamv2.entity.Mistake;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MistakeRepository extends JpaRepository<Mistake, Long> {
    Page<Mistake> findByStatus(MistakeStatus status, Pageable pageable);
}