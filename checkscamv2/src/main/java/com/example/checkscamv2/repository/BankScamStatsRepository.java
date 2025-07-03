package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.BankScamStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.example.checkscamv2.dto.response.TopScamItemResponseDTO;

@Repository
public interface BankScamStatsRepository extends JpaRepository<BankScamStats, Long> {
    @Query("SELECT b FROM BankScamStats b WHERE b.bankScam.bankAccount = :bankAccount")
    Optional<BankScamStats> findByBankAccount(@Param("bankAccount") String bankAccount);

    /**
     * Lấy top bank accounts - loại trừ số điện thoại và ví điện tử dùng số điện thoại
     * Loại trừ:
     * - Số điện thoại (bắt đầu bằng 0 và có 10 chữ số)
     * - Ví Momo, ZaloPay, ShopeePay (thường dùng số điện thoại)
     */
    @Query("""
              SELECT new com.example.checkscamv2.dto.response.TopScamItemResponseDTO(
                       scam.id,
                       scam.bankAccount,
                       scam.bankName,
                       scam.nameAccount,
                       stats.verifiedCount,
                       COALESCE(stats.viewCount, 0),
                       stats.lastReportAt)
              FROM BankScamStats stats
              JOIN stats.bankScam scam
              WHERE NOT (LENGTH(scam.bankAccount) = 10 AND scam.bankAccount LIKE '0%')
              AND NOT (scam.bankName LIKE '%Momo%' OR scam.bankName LIKE '%Ví Momo%')
              AND NOT (scam.bankName LIKE '%ZaloPay%' OR scam.bankName LIKE '%Ví ZaloPay%')
              AND NOT (scam.bankName LIKE '%ShopeePay%' OR scam.bankName LIKE '%Ví ShopeePay%')
              ORDER BY COALESCE(stats.viewCount, 0) DESC
              LIMIT 10
            """)
    List<TopScamItemResponseDTO> getTopBanksByViews();

    @Query("""
              SELECT new com.example.checkscamv2.dto.response.TopScamItemResponseDTO(
                       scam.id,
                       scam.bankAccount,
                       scam.bankName,
                       scam.nameAccount,
                       stats.verifiedCount,
                       COALESCE(stats.viewCount, 0),
                       stats.lastReportAt)
              FROM BankScamStats stats
              JOIN stats.bankScam scam
              WHERE NOT (LENGTH(scam.bankAccount) = 10 AND scam.bankAccount LIKE '0%')
              AND NOT (scam.bankName LIKE '%Momo%' OR scam.bankName LIKE '%Ví Momo%')
              AND NOT (scam.bankName LIKE '%ZaloPay%' OR scam.bankName LIKE '%Ví ZaloPay%')
              AND NOT (scam.bankName LIKE '%ShopeePay%' OR scam.bankName LIKE '%Ví ShopeePay%')
              ORDER BY stats.verifiedCount DESC
              LIMIT 10
            """)
    List<TopScamItemResponseDTO> getTopBanksByReports();
    
    /**
     * Tìm các bank accounts thực chất là số điện thoại (để cleanup data)
     */
    @Query("""
              SELECT scam.bankAccount
              FROM BankScamStats stats
              JOIN stats.bankScam scam
              WHERE LENGTH(scam.bankAccount) = 10 
              AND scam.bankAccount LIKE '0%'
            """)
    List<String> findPhoneNumbersInBankData();
    
    /**
     * Tìm các ví điện tử sử dụng số điện thoại
     */
    @Query("""
              SELECT scam.bankAccount
              FROM BankScamStats stats
              JOIN stats.bankScam scam
              WHERE (scam.bankName LIKE '%Momo%' OR scam.bankName LIKE '%Ví Momo%'
                  OR scam.bankName LIKE '%ZaloPay%' OR scam.bankName LIKE '%Ví ZaloPay%'
                  OR scam.bankName LIKE '%ShopeePay%' OR scam.bankName LIKE '%Ví ShopeePay%')
              AND LENGTH(scam.bankAccount) = 10 
              AND scam.bankAccount LIKE '0%'
            """)
    List<String> findEWalletPhoneNumbers();
}