package com.example.checkscamv2.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "scam_type")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ScamType extends BaseEntity {
    @Column(name = "name")
    private String name;

    @Column(name = "code")
    private String code;
}
