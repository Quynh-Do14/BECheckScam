package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.PhoneScamStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.example.checkscamv2.dto.response.TopScamItemResponseDTO;

@Repository
public interface PhoneScamStatsRepository extends JpaRepository<PhoneScamStats, Long> {
    @Query("SELECT p FROM PhoneScamStats p WHERE p.phoneScam.phoneNumber = :phoneNumber")
    Optional<PhoneScamStats> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("""
              SELECT new com.example.checkscamv2.dto.response.TopScamItemResponseDTO(
                       scam.id,
                       scam.phoneNumber,
                       scam.ownerName,
                       stats.verifiedCount,
                       COALESCE(stats.viewCount, 0),
                       stats.lastReportAt)
              FROM PhoneScamStats stats
              JOIN stats.phoneScam scam
              ORDER BY COALESCE(stats.viewCount, 0) DESC
              LIMIT 10
            """)
    List<TopScamItemResponseDTO> getTopPhonesByViews();

    @Query("""
              SELECT new com.example.checkscamv2.dto.response.TopScamItemResponseDTO(
                       scam.id,
                       scam.phoneNumber,
                       scam.ownerName,
                       stats.verifiedCount,
                       COALESCE(stats.viewCount, 0),
                       stats.lastReportAt)
              FROM PhoneScamStats stats
              JOIN stats.phoneScam scam
              ORDER BY stats.verifiedCount DESC
              LIMIT 10
            """)
    List<TopScamItemResponseDTO> getTopPhonesByReports();

}