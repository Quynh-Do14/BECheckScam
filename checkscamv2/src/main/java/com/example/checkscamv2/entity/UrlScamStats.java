package com.example.checkscamv2.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "url_scam_stats",
        indexes = {
                @Index(name = "idx_url_cnt", columnList = "verified_count desc"),
                @Index(name = "idx_url_last", columnList = "last_report_at desc"),
                @Index(name = "idx_url_view", columnList = "view_count desc")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlScamStats {

    @Id
    @Column(name = "url_scam_id")
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "url_scam_id")
    private UrlScam urlScam;

    @Column(name = "verified_count", nullable = false)
    private Integer verifiedCount = 0;

    @Column(name = "last_report_at")
    private LocalDateTime lastReportAt;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;
}