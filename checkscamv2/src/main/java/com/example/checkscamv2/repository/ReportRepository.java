package com.example.checkscamv2.repository;

import com.example.checkscamv2.dto.MonthlyReportStatsDTO;
import com.example.checkscamv2.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // OLD JPQL - CAUSES COLLATION ERROR
    // @Query("""
    //         SELECT u.id,
    //                r.emailAuthorReport,
    //                u.name,
    //                SUM(CASE WHEN r.status = 2 THEN 1 ELSE 0 END) as approvedCount,
    //                COUNT(r) as totalCount,
    //                MIN(r.dateReport) as firstReport,
    //                MAX(r.dateReport) as lastReport
    //         FROM Report r
    //         LEFT JOIN User u ON r.emailAuthorReport = u.email
    //         WHERE r.emailAuthorReport IS NOT NULL
    //         GROUP BY u.id, r.emailAuthorReport, u.name
    //         HAVING COUNT(r) > 0
    //         ORDER BY SUM(CASE WHEN r.status = 2 THEN 1 ELSE 0 END) DESC
    //         """)
    // List<Object[]> findAllReporterRankingData();

    // NEW NATIVE QUERY - FIXES COLLATION ISSUE
    @Query(value = """
            SELECT u.id,
                   r.email_author_report,
                   u.name,
                   SUM(CASE WHEN r.status = 2 THEN 1 ELSE 0 END) as approved_count,
                   COUNT(r.id) as total_count,
                   MIN(r.date_report) as first_report,
                   MAX(r.date_report) as last_report
            FROM report r
            LEFT JOIN users u ON r.email_author_report COLLATE utf8mb4_unicode_ci = u.email COLLATE utf8mb4_unicode_ci
            WHERE r.email_author_report IS NOT NULL
            GROUP BY u.id, r.email_author_report, u.name
            HAVING COUNT(r.id) > 0
            ORDER BY approved_count DESC
            """, nativeQuery = true)
    List<Object[]> findAllReporterRankingData();

    @Query("""
            SELECT COUNT(DISTINCT r.emailAuthorReport)
            FROM Report r
            WHERE r.emailAuthorReport IS NOT NULL
            """)
    Long countTotalReporters();

    @Query("""
            SELECT COUNT(r)
            FROM Report r
            WHERE r.status = 2
            """)
    Long countTotalApprovedReports();

    @Query("SELECT COUNT(r) FROM Report r WHERE r.emailAuthorReport = :email")
    long countByEmail(@Param("email") String email);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.emailAuthorReport = :email AND r.status = :status")
    long countByEmailAndStatus(@Param("email") String email, @Param("status") Integer status);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.status = :status")
    long countByStatus(@Param("status") Integer status);

    // OLD JPQL VERSION - NO JOIN SO IT SHOULD WORK
    @Query("""
            SELECT r.emailAuthorReport,
                   SUM(CASE WHEN r.status = 2 THEN 1 ELSE 0 END) as approvedCount,
                   COUNT(r) as totalCount
            FROM Report r
            WHERE r.emailAuthorReport IS NOT NULL
            GROUP BY r.emailAuthorReport
            HAVING COUNT(r) > 0
            """)
    List<Object[]> findAllReporterStats();

    @Query("""
              SELECT new com.example.checkscamv2.dto.MonthlyReportStatsDTO(
                CAST(MONTH(r.dateReport) AS integer),
                COUNT(r)
              )
              FROM Report r
              WHERE YEAR(r.dateReport) = :year
              GROUP BY MONTH(r.dateReport)
              ORDER BY MONTH(r.dateReport)
            """)
    List<MonthlyReportStatsDTO> findReportCountByMonth(@Param("year") Integer year);


    @Query("""
                SELECT YEAR(r.dateReport), COUNT(r)
                FROM Report r
                WHERE r.dateReport IS NOT NULL
                GROUP BY YEAR(r.dateReport)
                ORDER BY YEAR(r.dateReport)
            """)
    List<Object[]> findReportCountByYear();

}