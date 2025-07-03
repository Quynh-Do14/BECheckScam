package com.example.checkscamv2.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bank_scam_stats",
        indexes = {
                @Index(name = "idx_bank_cnt", columnList = "verified_count desc"),
                @Index(name = "idx_bank_last", columnList = "last_report_at desc"),
                @Index(name = "idx_bank_view", columnList = "view_count desc")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankScamStats {

    @Id
    @Column(name = "bank_scam_id")
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "bank_scam_id")
    private BankScam bankScam;

    @Column(name = "verified_count", nullable = false)
    private Integer verifiedCount = 0;

    @Column(name = "last_report_at")
    private LocalDateTime lastReportAt;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;
}