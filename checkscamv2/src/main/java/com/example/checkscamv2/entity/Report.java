package com.example.checkscamv2.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "report",
        indexes = {
                @Index(name = "idx_report_date", columnList = "date_report desc")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "email_author_report")
    private String emailAuthorReport;

    @Column(name = "status")
    private Integer status;

    @Column(name = "date_report")
    private LocalDateTime dateReport;

    @Column(name = "money_scam")
    private String moneyScam;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<ReportDetail> reportDetails;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Attachment> attachments;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}