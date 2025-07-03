package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.UrlScamStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import com.example.checkscamv2.dto.response.TopScamItemResponseDTO;

@Repository
public interface UrlScamStatsRepository extends JpaRepository<UrlScamStats, Long> {

    @Query("SELECT u FROM UrlScamStats u WHERE u.urlScam.url = :url")
    Optional<UrlScamStats> findByUrl(@Param("url") String url);
    @Query("""
              SELECT new com.example.checkscamv2.dto.response.TopScamItemResponseDTO(
                       scam.id,
                       scam.url,
                       stats.verifiedCount,
                       COALESCE(stats.viewCount, 0),
                       stats.lastReportAt)
              FROM UrlScamStats stats
              JOIN stats.urlScam scam
              ORDER BY COALESCE(stats.viewCount, 0) DESC
              LIMIT 10
            """)
    List<TopScamItemResponseDTO> getTopUrlsByViews();

    @Query("""
              SELECT new com.example.checkscamv2.dto.response.TopScamItemResponseDTO(
                       scam.id,
                       scam.url,
                       stats.verifiedCount,
                       COALESCE(stats.viewCount, 0),
                       stats.lastReportAt)
              FROM UrlScamStats stats
              JOIN stats.urlScam scam
              ORDER BY stats.verifiedCount DESC
              LIMIT 10
            """)
    List<TopScamItemResponseDTO> getTopUrlsByReports();
}