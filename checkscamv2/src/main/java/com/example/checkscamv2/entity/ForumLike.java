package com.example.checkscamv2.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "forum_likes", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "post_id"}),
           @UniqueConstraint(columnNames = {"user_id", "comment_id"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumLike extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private ForumPost post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private ForumComment comment;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Ensure either post or comment is not null, but not both
    @PrePersist
    @PreUpdate
    private void validateLike() {
        if ((post == null && comment == null) || (post != null && comment != null)) {
            throw new IllegalStateException("Like must be associated with either a post or a comment, but not both");
        }
    }
}
