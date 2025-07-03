package com.example.checkscamv2.entity;

import com.example.checkscamv2.constant.MistakeDetailType; // ĐÃ THAY ĐỔI IMPORT
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mistake_detail",
        indexes = @Index(name = "idx_mistake_detail_type", columnList = "type"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MistakeDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mistake_id", nullable = false)
    @JsonBackReference(value = "mistake-mistakedetails")
    private Mistake mistake;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MistakeDetailType type; // ĐÃ THAY ĐỔI KIỂU DỮ LIỆU

    @Column(name = "info", nullable = false, columnDefinition = "text")
    private String info;
}