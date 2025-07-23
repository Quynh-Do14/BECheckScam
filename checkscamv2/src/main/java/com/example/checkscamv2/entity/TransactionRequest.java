package com.example.checkscamv2.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_requests")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;

    @Column(name = "dealer_name", nullable = false)
    private String dealerName;

    @Column(name = "dealer_email", nullable = false)
    private String dealerEmail;

    @Column(name = "party_a_name", nullable = false)
    private String partyAName;

    @Column(name = "party_a_email", nullable = false)
    private String partyAEmail;

    @Column(name = "party_a_phone", nullable = false)
    private String partyAPhone;

    @Column(name = "party_b_name", nullable = false)
    private String partyBName;

    @Column(name = "party_b_email", nullable = false)
    private String partyBEmail;

    @Column(name = "party_b_phone", nullable = false)
    private String partyBPhone;

    @Column(name = "room_name", nullable = false)
    private String roomName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "email_sent", nullable = false)
    private boolean emailSent;

    @Column(name = "status")
    private String status = "PENDING"; // PENDING, IN_PROGRESS, COMPLETED, CANCELLED

    // Lifecycle callback
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }


}
