package com.luna.skin.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "skin_concern")
@Getter
@NoArgsConstructor
public class SkinConcern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skin_concern_id")
    private Long skinConcernId;

    @Column(name = "concern_name", nullable = false, length = 50)
    private String concernName;
}
