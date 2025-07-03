package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.MistakeDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MistakeDetailRepository extends JpaRepository<MistakeDetail, Long> {
}