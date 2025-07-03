package com.example.checkscamv2.entity;

import com.example.checkscamv2.constant.MistakeStatus;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "mistake",
        indexes = {
                @Index(name = "idx_mistake_status", columnList = "status"),
                @Index(name = "idx_mistake_date", columnList = "date_mistake desc")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mistake {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint_reason", columnDefinition = "text")
    private String complaintReason;

    @Column(name = "email_author_mistake", nullable = false)
    private String emailAuthorMistake;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MistakeStatus status;

    @Column(name = "date_mistake", nullable = false)
    private LocalDateTime dateMistake;

    @OneToMany(mappedBy = "mistake", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference(value = "mistake-mistakedetails")
    private List<MistakeDetail> mistakeDetails;

    @OneToMany(mappedBy = "mistake", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference(value = "mistake-attachments")
    private List<Attachment> attachments;

}