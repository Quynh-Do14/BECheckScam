package com.example.checkscamv2.service.Impl;


import com.example.checkscamv2.dto.request.TransactionRequestDTO;
import com.example.checkscamv2.dto.response.TransactionResponseDTO;
import com.example.checkscamv2.entity.TransactionRequest;
import com.example.checkscamv2.repository.TransactionRequestRepository;
import com.example.checkscamv2.service.EmailService;
import com.example.checkscamv2.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);

    // Patterns for validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10,11}$");

    @Autowired
    private TransactionRequestRepository repository;

    @Autowired
    private EmailService emailService;

    @Override
    @Transactional
    public TransactionResponseDTO createTransactionRequest(TransactionRequestDTO requestDTO) {
        logger.info("Bắt đầu tạo yêu cầu giao dịch cho dealer: {}", requestDTO.getDealerEmail());

        try {
            // Validate data
            if (!validateTransactionData(requestDTO)) {
                throw new IllegalArgumentException("Dữ liệu giao dịch không hợp lệ");
            }

            // Tạo transaction ID unique
            String transactionId = requestDTO.getTransactionCode();

            // Chuyển đổi DTO sang Entity
            TransactionRequest entity = convertToEntity(requestDTO, transactionId);

            // Gửi email thông báo cho giao dịch viên
            boolean emailSent = emailService.sendTransactionNotification(requestDTO, transactionId);
            entity.setEmailSent(emailSent);

            // Gửi email xác nhận cho hai bên (có thể tùy chọn)
            emailService.sendConfirmationEmails(requestDTO, transactionId);

            // Lưu vào database
            TransactionRequest savedEntity = repository.save(entity);

            logger.info("Tạo giao dịch thành công: {} - Email sent: {}", transactionId, emailSent);

            String message = emailSent ?
                    "Yêu cầu giao dịch đã được tạo thành công và email đã được gửi đến giao dịch viên" :
                    "Yêu cầu giao dịch đã được tạo thành công nhưng không thể gửi email. Vui lòng liên hệ trực tiếp với giao dịch viên";

            return new TransactionResponseDTO(transactionId, message, LocalDateTime.now(), emailSent);

        } catch (Exception e) {
            logger.error("Lỗi khi tạo yêu cầu giao dịch: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo yêu cầu giao dịch: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TransactionRequest> getTransactionById(String transactionId) {
        return repository.findByTransactionId(transactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionRequest> getTransactionsByDealer(String dealerEmail) {
        return repository.findByDealerEmailOrderByCreatedAtDesc(dealerEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionRequest> getTransactionsByDealer(String dealerEmail, Pageable pageable) {
        return repository.findByDealerEmailOrderByCreatedAtDesc(dealerEmail, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionRequest> getTransactionsByStatus(String status) {
        return repository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionRequest> getTransactionsByPartyEmail(String email) {
        return repository.findByPartyEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionRequest> getTransactionsByPartyPhone(String phone) {
        return repository.findByPartyPhone(phone);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionRequest> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return repository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate);
    }

    @Override
    @Transactional
    public boolean updateTransactionStatus(String transactionId, String newStatus) {
        return updateTransactionStatus(transactionId, newStatus, "SYSTEM", null);
    }

    @Override
    @Transactional
    public boolean updateTransactionStatus(String transactionId, String newStatus, String updatedBy, String notes) {
        Optional<TransactionRequest> optionalTransaction = repository.findByTransactionId(transactionId);

        if (optionalTransaction.isPresent()) {
            TransactionRequest transaction = optionalTransaction.get();
            String oldStatus = transaction.getStatus();

            transaction.setStatus(newStatus);
            repository.save(transaction);

            // Gửi email thông báo cập nhật trạng thái
            try {
                TransactionRequestDTO dto = convertToDTO(transaction);
                emailService.sendStatusUpdateNotification(dto, transactionId, oldStatus, newStatus);
            } catch (Exception e) {
                logger.warn("Không thể gửi email thông báo cập nhật trạng thái: {}", e.getMessage());
            }

            logger.info("Cập nhật status giao dịch {} từ {} thành {} bởi {}",
                    transactionId, oldStatus, newStatus, updatedBy);
            return true;
        }

        logger.warn("Không tìm thấy giao dịch với ID: {}", transactionId);
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionStatistics getTransactionStatistics() {
        long totalTransactions = repository.count();
        long pendingTransactions = repository.countByStatus("PENDING");
        long inProgressTransactions = repository.countByStatus("IN_PROGRESS");
        long completedTransactions = repository.countByStatus("COMPLETED");
        long cancelledTransactions = repository.countByStatus("CANCELLED");

        // Thống kê theo thời gian
        LocalDateTime today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        LocalDateTime startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDateTime startOfMonth = today.withDayOfMonth(1);

        long todayTransactions = repository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                today, today.plusDays(1)).size();
        long thisWeekTransactions = repository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                startOfWeek, today.plusDays(1)).size();
        long thisMonthTransactions = repository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                startOfMonth, today.plusDays(1)).size();

        return new TransactionStatistics(
                totalTransactions,
                pendingTransactions,
                inProgressTransactions,
                completedTransactions,
                cancelledTransactions,
                todayTransactions,
                thisWeekTransactions,
                thisMonthTransactions
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DealerStatistics getDealerStatistics(String dealerEmail) {
        List<TransactionRequest> dealerTransactions = repository.findByDealerEmailOrderByCreatedAtDesc(dealerEmail);

        if (dealerTransactions.isEmpty()) {
            return new DealerStatistics(dealerEmail, "", 0, 0, 0, 0, 0, null, 0.0);
        }

        String dealerName = dealerTransactions.get(0).getDealerName();
        long totalTransactions = dealerTransactions.size();
        long completedTransactions = dealerTransactions.stream()
                .mapToLong(t -> "COMPLETED".equals(t.getStatus()) ? 1 : 0)
                .sum();

        LocalDateTime today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        LocalDateTime startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDateTime startOfMonth = today.withDayOfMonth(1);

        long todayTransactions = dealerTransactions.stream()
                .mapToLong(t -> t.getCreatedAt().isAfter(today) ? 1 : 0)
                .sum();
        long thisWeekTransactions = dealerTransactions.stream()
                .mapToLong(t -> t.getCreatedAt().isAfter(startOfWeek) ? 1 : 0)
                .sum();
        long thisMonthTransactions = dealerTransactions.stream()
                .mapToLong(t -> t.getCreatedAt().isAfter(startOfMonth) ? 1 : 0)
                .sum();

        LocalDateTime lastTransactionDate = dealerTransactions.get(0).getCreatedAt();

        // Tính trung bình giao dịch mỗi ngày
        LocalDateTime firstTransactionDate = dealerTransactions.get(dealerTransactions.size() - 1).getCreatedAt();
        long daysBetween = ChronoUnit.DAYS.between(firstTransactionDate, LocalDateTime.now());
        double averageTransactionsPerDay = daysBetween > 0 ? (double) totalTransactions / daysBetween : totalTransactions;

        return new DealerStatistics(
                dealerEmail,
                dealerName,
                totalTransactions,
                completedTransactions,
                todayTransactions,
                thisWeekTransactions,
                thisMonthTransactions,
                lastTransactionDate,
                averageTransactionsPerDay
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long getTodayTransactionsByDealer(String dealerEmail) {
        return repository.countTodayTransactionsByDealer(dealerEmail);
    }

    @Override
    @Transactional
    public int resendFailedEmails() {
        List<TransactionRequest> failedTransactions = repository.findByEmailSentFalseOrderByCreatedAtAsc();
        int successCount = 0;

        for (TransactionRequest transaction : failedTransactions) {
            try {
                TransactionRequestDTO dto = convertToDTO(transaction);
                boolean emailSent = emailService.sendTransactionNotification(dto, transaction.getTransactionId());

                if (emailSent) {
                    transaction.setEmailSent(true);
                    repository.save(transaction);
                    successCount++;
                }
            } catch (Exception e) {
                logger.error("Lỗi khi gửi lại email cho transaction {}: {}",
                        transaction.getTransactionId(), e.getMessage());
            }
        }

        logger.info("Đã gửi lại thành công {} email từ {} giao dịch", successCount, failedTransactions.size());
        return successCount;
    }

    @Override
    @Transactional
    public boolean deleteTransaction(String transactionId, String deletedBy) {
        Optional<TransactionRequest> optionalTransaction = repository.findByTransactionId(transactionId);

        if (optionalTransaction.isPresent()) {
            TransactionRequest transaction = optionalTransaction.get();
            transaction.setStatus("DELETED");
            repository.save(transaction);

            logger.info("Giao dịch {} đã được xóa bởi {}", transactionId, deletedBy);
            return true;
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionRequest> searchTransactions(String keyword, Pageable pageable) {
        // Implement search logic - có thể search theo tên, email, phone, transaction ID
        // Sử dụng Specification hoặc custom query
        throw new UnsupportedOperationException("Search functionality not implemented yet");
    }

    @Override
    public boolean validateTransactionData(TransactionRequestDTO requestDTO) {
        try {
            // Validate dealer info
            if (!StringUtils.hasText(requestDTO.getDealerName()) ||
                    !StringUtils.hasText(requestDTO.getDealerEmail()) ||
                    !StringUtils.hasText(requestDTO.getDealerPhone())) {
                logger.error("Thông tin giao dịch viên không đầy đủ");
                return false;
            }

            // Validate email formats
            if (!EMAIL_PATTERN.matcher(requestDTO.getDealerEmail()).matches() ||
                    !EMAIL_PATTERN.matcher(requestDTO.getPartyAEmail()).matches() ||
                    !EMAIL_PATTERN.matcher(requestDTO.getPartyBEmail()).matches()) {
                logger.error("Định dạng email không hợp lệ");
                return false;
            }

            // Validate phone formats
            if (!PHONE_PATTERN.matcher(requestDTO.getDealerPhone()).matches() ||
                    !PHONE_PATTERN.matcher(requestDTO.getPartyAPhone()).matches() ||
                    !PHONE_PATTERN.matcher(requestDTO.getPartyBPhone()).matches()) {
                logger.error("Định dạng số điện thoại không hợp lệ");
                return false;
            }

            // Validate party info
            if (!StringUtils.hasText(requestDTO.getPartyAName()) ||
                    !StringUtils.hasText(requestDTO.getPartyBName()) ||
                    !StringUtils.hasText(requestDTO.getRoomName())) {
                logger.error("Thông tin các bên hoặc phòng không đầy đủ");
                return false;
            }

            // Check for duplicate emails/phones
            if (requestDTO.getPartyAEmail().equals(requestDTO.getPartyBEmail()) ||
                    requestDTO.getPartyAPhone().equals(requestDTO.getPartyBPhone())) {
                logger.error("Email hoặc số điện thoại của hai bên không được trùng nhau");
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.error("Lỗi khi validate dữ liệu: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Tạo transaction ID unique
     */
    private String generateTransactionId() {
        String prefix = "TXN";
        String timestamp = String.valueOf(System.currentTimeMillis() % 100000);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + "-" + timestamp + "-" + random;
    }

    /**
     * Chuyển đổi DTO sang Entity
     */
    private TransactionRequest convertToEntity(TransactionRequestDTO dto, String transactionId) {
        TransactionRequest entity = new TransactionRequest();
        entity.setTransactionId(transactionId);
        entity.setDealerName(dto.getDealerName());
        entity.setDealerEmail(dto.getDealerEmail());
        entity.setDealerPhone(dto.getDealerPhone());
        entity.setPartyAName(dto.getPartyAName());
        entity.setPartyAEmail(dto.getPartyAEmail());
        entity.setPartyAPhone(dto.getPartyAPhone());
        entity.setPartyBName(dto.getPartyBName());
        entity.setPartyBEmail(dto.getPartyBEmail());
        entity.setPartyBPhone(dto.getPartyBPhone());
        entity.setRoomName(dto.getRoomName());
        entity.setStatus("PENDING");
        return entity;
    }

    /**
     * Chuyển đổi Entity sang DTO
     */
    private TransactionRequestDTO convertToDTO(TransactionRequest entity) {
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setDealerName(entity.getDealerName());
        dto.setDealerEmail(entity.getDealerEmail());
        dto.setDealerPhone(entity.getDealerPhone());
        dto.setPartyAName(entity.getPartyAName());
        dto.setPartyAEmail(entity.getPartyAEmail());
        dto.setPartyAPhone(entity.getPartyAPhone());
        dto.setPartyBName(entity.getPartyBName());
        dto.setPartyBEmail(entity.getPartyBEmail());
        dto.setPartyBPhone(entity.getPartyBPhone());
        dto.setRoomName(entity.getRoomName());
        return dto;
    }
}
