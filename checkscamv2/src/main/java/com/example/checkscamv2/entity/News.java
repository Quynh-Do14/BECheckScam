package com.example.checkscamv2.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "news")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News extends BaseEntity {
    @Column(nullable = false, length = 500)
    private String name;
    
    @Column(columnDefinition = "LONGTEXT")
    private String shortDescription;
    
    @Column(columnDefinition = "LONGTEXT")
    private String content;
    
    @Column(name = "is_main_news", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isMainNews = false;

    @OneToMany(mappedBy = "news", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference(value = "news-attachments")
    private List<Attachment> attachments = new ArrayList<>();
}