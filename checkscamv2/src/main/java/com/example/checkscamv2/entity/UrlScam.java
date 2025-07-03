package com.example.checkscamv2.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "url_scam",
        indexes = @Index(name = "idx_url", columnList = "url"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlScam extends BaseEntity {

    @Column(name = "url", length = 512, nullable = false)
    private String url;

    @OneToOne(mappedBy = "urlScam", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UrlScamStats stats;
}
