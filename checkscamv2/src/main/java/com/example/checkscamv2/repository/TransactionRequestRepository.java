package com.example.checkscamv2.repository;


import com.example.checkscamv2.entity.TransactionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRequestRepository extends JpaRepository<TransactionRequest, Long> {

    // Tìm theo transaction ID
    Optional<TransactionRequest> findByTransactionId(String transactionId);

    // Tìm theo email giao dịch viên
    List<TransactionRequest> findByDealerEmailOrderByCreatedAtDesc(String dealerEmail);

    // Tìm theo status
    List<TransactionRequest> findByStatusOrderByCreatedAtDesc(String status);

    // Tìm theo khoảng thời gian
    List<TransactionRequest> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // Tìm theo email của một trong hai bên
    @Query("SELECT t FROM TransactionRequest t WHERE t.partyAEmail = :email OR t.partyBEmail = :email ORDER BY t.createdAt DESC")
    List<TransactionRequest> findByPartyEmail(@Param("email") String email);

    // Tìm theo số điện thoại của một trong hai bên
    @Query("SELECT t FROM TransactionRequest t WHERE t.partyAPhone = :phone OR t.partyBPhone = :phone ORDER BY t.createdAt DESC")
    List<TransactionRequest> findByPartyPhone(@Param("phone") String phone);

    // Phân trang cho giao dịch viên
    Page<TransactionRequest> findByDealerEmailOrderByCreatedAtDesc(String dealerEmail, Pageable pageable);

    // Đếm số giao dịch theo status
    long countByStatus(String status);

    // Đếm số giao dịch của giao dịch viên trong ngày
    @Query("SELECT COUNT(t) FROM TransactionRequest t WHERE t.dealerEmail = :dealerEmail AND DATE(t.createdAt) = CURRENT_DATE")
    long countTodayTransactionsByDealer(@Param("dealerEmail") String dealerEmail);

    // Tìm giao dịch chưa gửi email
    List<TransactionRequest> findByEmailSentFalseOrderByCreatedAtAsc();
}