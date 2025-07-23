package com.example.checkscamv2.controller;


import com.example.checkscamv2.dto.request.TransactionRequestDTO;
import com.example.checkscamv2.dto.response.TransactionResponseDTO;
import com.example.checkscamv2.entity.TransactionRequest;
import com.example.checkscamv2.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionService transactionService;

    /**
     * Tạo yêu cầu giao dịch mới
     */
    @PostMapping("/create")
    public ResponseEntity<?> createTransaction(
            @Valid @RequestBody TransactionRequestDTO request,
            BindingResult bindingResult) {

        logger.info("Nhận yêu cầu tạo giao dịch mới từ dealer: {}", request.getDealerEmail());

        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage()));

            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Dữ liệu không hợp lệ", errors));
        }

        try {
            TransactionResponseDTO response = transactionService.createTransactionRequest(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Lỗi khi tạo giao dịch: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi hệ thống", e.getMessage()));
        }
    }

    /**
     * Lấy thông tin giao dịch theo ID
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<?> getTransaction(@PathVariable String transactionId) {

        Optional<TransactionRequest> transaction = transactionService.getTransactionById(transactionId);

        if (transaction.isPresent()) {
            return ResponseEntity.ok(transaction.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Lấy danh sách giao dịch của giao dịch viên
     */
    @GetMapping("/dealer/{dealerEmail}")
    public ResponseEntity<List<TransactionRequest>> getTransactionsByDealer(
            @PathVariable String dealerEmail) {

        List<TransactionRequest> transactions = transactionService.getTransactionsByDealer(dealerEmail);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Lấy danh sách giao dịch với phân trang
     */
    @GetMapping("/dealer/{dealerEmail}/paged")
    public ResponseEntity<Page<TransactionRequest>> getTransactionsByDealerPaged(
            @PathVariable String dealerEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionRequest> transactions = transactionService.getTransactionsByDealer(dealerEmail, pageable);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Lấy danh sách giao dịch theo status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TransactionRequest>> getTransactionsByStatus(
            @PathVariable String status) {

        List<TransactionRequest> transactions = transactionService.getTransactionsByStatus(status);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Tìm giao dịch theo email
     */
    @GetMapping("/search/email/{email}")
    public ResponseEntity<List<TransactionRequest>> getTransactionsByEmail(
            @PathVariable String email) {

        List<TransactionRequest> transactions = transactionService.getTransactionsByPartyEmail(email);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Tìm giao dịch theo số điện thoại
     */
    @GetMapping("/search/phone/{phone}")
    public ResponseEntity<List<TransactionRequest>> getTransactionsByPhone(
            @PathVariable String phone) {

        List<TransactionRequest> transactions = transactionService.getTransactionsByPartyPhone(phone);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Cập nhật status giao dịch
     */
    @PutMapping("/{transactionId}/status")
    public ResponseEntity<?> updateTransactionStatus(
            @PathVariable String transactionId,
            @RequestBody Map<String, String> statusUpdate) {

        String newStatus = statusUpdate.get("status");
        if (newStatus == null || newStatus.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Status không được để trống", null));
        }

        // Validate status values
        if (!isValidStatus(newStatus)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Status không hợp lệ",
                            "Chỉ chấp nhận: PENDING, IN_PROGRESS, COMPLETED, CANCELLED"));
        }

        boolean updated = transactionService.updateTransactionStatus(transactionId, newStatus);

        if (updated) {
            return ResponseEntity.ok(Map.of(
                    "message", "Cập nhật status thành công",
                    "transactionId", transactionId,
                    "newStatus", newStatus
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Lấy thống kê giao dịch
     */
    @GetMapping("/statistics")
    public ResponseEntity<TransactionService.TransactionStatistics> getStatistics() {
        TransactionService.TransactionStatistics stats = transactionService.getTransactionStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Lấy thống kê chi tiết của giao dịch viên
     */
    @GetMapping("/dealer/{dealerEmail}/statistics")
    public ResponseEntity<TransactionService.DealerStatistics> getDealerStatistics(
            @PathVariable String dealerEmail) {

        TransactionService.DealerStatistics stats = transactionService.getDealerStatistics(dealerEmail);
        return ResponseEntity.ok(stats);
    }

    /**
     * Lấy số giao dịch hôm nay của giao dịch viên
     */
    @GetMapping("/dealer/{dealerEmail}/today-count")
    public ResponseEntity<Map<String, Object>> getTodayTransactionCount(
            @PathVariable String dealerEmail) {

        long count = transactionService.getTodayTransactionsByDealer(dealerEmail);
        return ResponseEntity.ok(Map.of(
                "dealerEmail", dealerEmail,
                "todayTransactionCount", count
        ));
    }

    /**
     * Gửi lại email cho các giao dịch chưa gửi được
     */
    @PostMapping("/resend-emails")
    public ResponseEntity<Map<String, Object>> resendFailedEmails() {
        int successCount = transactionService.resendFailedEmails();
        return ResponseEntity.ok(Map.of(
                "message", "Đã gửi lại email thành công",
                "successCount", successCount
        ));
    }

    /**
     * Kiểm tra tính hợp lệ của status
     */
    private boolean isValidStatus(String status) {
        return status.equals("PENDING") ||
                status.equals("IN_PROGRESS") ||
                status.equals("COMPLETED") ||
                status.equals("CANCELLED");
    }

    /**
     * Class cho error response
     */
    public static class ErrorResponse {
        private String error;
        private Object details;

        public ErrorResponse(String error, Object details) {
            this.error = error;
            this.details = details;
        }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public Object getDetails() { return details; }
        public void setDetails(Object details) { this.details = details; }
    }
}
