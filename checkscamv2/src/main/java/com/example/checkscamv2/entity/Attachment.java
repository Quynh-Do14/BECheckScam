package com.example.checkscamv2.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attachment",
        indexes = {
                @Index(name = "idx_attachment_report_id", columnList = "report_id"),
                @Index(name = "idx_attachment_news_id", columnList = "news_id"),
                @Index(name = "idx_attachment_mistake_id", columnList = "mistake_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment extends BaseEntity {

    @Column(columnDefinition = "text", nullable = false)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    @JsonBackReference(value = "report-attachments")
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id")
    @JsonBackReference(value = "news-attachments")
    private News news;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mistake_id")
    @JsonBackReference(value = "mistake-attachments")
    private Mistake mistake;
}