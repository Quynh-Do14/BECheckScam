package com.example.checkscamv2.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "report_detail",
        indexes = {
                @Index(name = "idx_report_detail_type", columnList = "type"),
                @Index(name = "idx_report_detail_info", columnList = "info")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDetail extends BaseEntity {
    @Column(name = "type", nullable = false)
    private Integer type;  // 1: phone, 2: bank, 3: url

    @Column(name = "info", nullable = false)
    private String info;  // Số điện thoại, số tài khoản, hoặc URL

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "status")
    private Integer status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    @JsonBackReference
    private Report report;
}