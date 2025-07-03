package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.UrlScam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlScamRepository extends JpaRepository<UrlScam, Long> {
    Optional<UrlScam> findByUrl(String url);
}