package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.request.TransactionRequestDTO;
import com.example.checkscamv2.dto.response.TransactionResponseDTO;
import com.example.checkscamv2.entity.TransactionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Interface cho Transaction Service
 */
public interface TransactionService {

    /**
     * Tạo yêu cầu giao dịch mới
     * @param requestDTO thông tin giao dịch
     * @return response chứa thông tin kết quả
     */
    TransactionResponseDTO createTransactionRequest(TransactionRequestDTO requestDTO);

    /**
     * Lấy thông tin giao dịch theo ID
     * @param transactionId mã giao dịch
     * @return thông tin giao dịch hoặc empty
     */
    Optional<TransactionRequest> getTransactionById(String transactionId);

    /**
     * Lấy danh sách giao dịch của một giao dịch viên
     * @param dealerEmail email giao dịch viên
     * @return danh sách giao dịch
     */
    List<TransactionRequest> getTransactionsByDealer(String dealerEmail);

    /**
     * Lấy danh sách giao dịch với phân trang
     * @param dealerEmail email giao dịch viên
     * @param pageable thông tin phân trang
     * @return page chứa danh sách giao dịch
     */
    Page<TransactionRequest> getTransactionsByDealer(String dealerEmail, Pageable pageable);

    /**
     * Lấy danh sách giao dịch theo status
     * @param status trạng thái giao dịch
     * @return danh sách giao dịch
     */
    List<TransactionRequest> getTransactionsByStatus(String status);

    /**
     * Tìm giao dịch theo email của một bên
     * @param email email tìm kiếm
     * @return danh sách giao dịch
     */
    List<TransactionRequest> getTransactionsByPartyEmail(String email);

    /**
     * Tìm giao dịch theo số điện thoại của một bên
     * @param phone số điện thoại tìm kiếm
     * @return danh sách giao dịch
     */
    List<TransactionRequest> getTransactionsByPartyPhone(String phone);

    /**
     * Tìm giao dịch theo khoảng thời gian
     * @param startDate ngày bắt đầu
     * @param endDate ngày kết thúc
     * @return danh sách giao dịch
     */
    List<TransactionRequest> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Cập nhật status giao dịch
     * @param transactionId mã giao dịch
     * @param newStatus trạng thái mới
     * @return true nếu cập nhật thành công
     */
    boolean updateTransactionStatus(String transactionId, String newStatus);

    /**
     * Cập nhật status với người thực hiện
     * @param transactionId mã giao dịch
     * @param newStatus trạng thái mới
     * @param updatedBy người cập nhật
     * @param notes ghi chú
     * @return true nếu cập nhật thành công
     */
    boolean updateTransactionStatus(String transactionId, String newStatus, String updatedBy, String notes);

    /**
     * Lấy thống kê giao dịch
     * @return thống kê chi tiết
     */
    TransactionStatistics getTransactionStatistics();

    /**
     * Lấy thống kê giao dịch theo giao dịch viên
     * @param dealerEmail email giao dịch viên
     * @return thống kê cá nhân
     */
    DealerStatistics getDealerStatistics(String dealerEmail);

    /**
     * Lấy số giao dịch hôm nay của giao dịch viên
     * @param dealerEmail email giao dịch viên
     * @return số lượng giao dịch
     */
    long getTodayTransactionsByDealer(String dealerEmail);

    /**
     * Gửi lại email cho các giao dịch chưa gửi được
     * @return số lượng email đã gửi thành công
     */
    int resendFailedEmails();

    /**
     * Xóa giao dịch (soft delete)
     * @param transactionId mã giao dịch
     * @param deletedBy người xóa
     * @return true nếu xóa thành công
     */
    boolean deleteTransaction(String transactionId, String deletedBy);

    /**
     * Tìm kiếm giao dịch theo từ khóa
     * @param keyword từ khóa tìm kiếm
     * @param pageable thông tin phân trang
     * @return page chứa kết quả tìm kiếm
     */
    Page<TransactionRequest> searchTransactions(String keyword, Pageable pageable);

    /**
     * Validate transaction data
     * @param requestDTO dữ liệu cần validate
     * @return true nếu hợp lệ
     */
    boolean validateTransactionData(TransactionRequestDTO requestDTO);

    /**
     * Class thống kê giao dịch tổng quan
     */
    class TransactionStatistics {
        private long totalTransactions;
        private long pendingTransactions;
        private long inProgressTransactions;
        private long completedTransactions;
        private long cancelledTransactions;
        private long todayTransactions;
        private long thisWeekTransactions;
        private long thisMonthTransactions;

        public TransactionStatistics(long totalTransactions, long pendingTransactions,
                                     long inProgressTransactions, long completedTransactions,
                                     long cancelledTransactions, long todayTransactions,
                                     long thisWeekTransactions, long thisMonthTransactions) {
            this.totalTransactions = totalTransactions;
            this.pendingTransactions = pendingTransactions;
            this.inProgressTransactions = inProgressTransactions;
            this.completedTransactions = completedTransactions;
            this.cancelledTransactions = cancelledTransactions;
            this.todayTransactions = todayTransactions;
            this.thisWeekTransactions = thisWeekTransactions;
            this.thisMonthTransactions = thisMonthTransactions;
        }

        // Getters
        public long getTotalTransactions() { return totalTransactions; }
        public long getPendingTransactions() { return pendingTransactions; }
        public long getInProgressTransactions() { return inProgressTransactions; }
        public long getCompletedTransactions() { return completedTransactions; }
        public long getCancelledTransactions() { return cancelledTransactions; }
        public long getTodayTransactions() { return todayTransactions; }
        public long getThisWeekTransactions() { return thisWeekTransactions; }
        public long getThisMonthTransactions() { return thisMonthTransactions; }

        public double getCompletionRate() {
            return totalTransactions > 0 ? (double) completedTransactions / totalTransactions * 100 : 0;
        }
    }

    /**
     * Class thống kê cá nhân giao dịch viên
     */
    class DealerStatistics {
        private String dealerEmail;
        private String dealerName;
        private long totalTransactions;
        private long completedTransactions;
        private long todayTransactions;
        private long thisWeekTransactions;
        private long thisMonthTransactions;
        private LocalDateTime lastTransactionDate;
        private double averageTransactionsPerDay;

        public DealerStatistics(String dealerEmail, String dealerName, long totalTransactions,
                                long completedTransactions, long todayTransactions,
                                long thisWeekTransactions, long thisMonthTransactions,
                                LocalDateTime lastTransactionDate, double averageTransactionsPerDay) {
            this.dealerEmail = dealerEmail;
            this.dealerName = dealerName;
            this.totalTransactions = totalTransactions;
            this.completedTransactions = completedTransactions;
            this.todayTransactions = todayTransactions;
            this.thisWeekTransactions = thisWeekTransactions;
            this.thisMonthTransactions = thisMonthTransactions;
            this.lastTransactionDate = lastTransactionDate;
            this.averageTransactionsPerDay = averageTransactionsPerDay;
        }

        // Getters
        public String getDealerEmail() { return dealerEmail; }
        public String getDealerName() { return dealerName; }
        public long getTotalTransactions() { return totalTransactions; }
        public long getCompletedTransactions() { return completedTransactions; }
        public long getTodayTransactions() { return todayTransactions; }
        public long getThisWeekTransactions() { return thisWeekTransactions; }
        public long getThisMonthTransactions() { return thisMonthTransactions; }
        public LocalDateTime getLastTransactionDate() { return lastTransactionDate; }
        public double getAverageTransactionsPerDay() { return averageTransactionsPerDay; }

        public double getCompletionRate() {
            return totalTransactions > 0 ? (double) completedTransactions / totalTransactions * 100 : 0;
        }
    }
}