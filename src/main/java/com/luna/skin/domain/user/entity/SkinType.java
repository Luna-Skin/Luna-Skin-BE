package com.luna.skin.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "skin_type")
@Getter
@NoArgsConstructor
public class SkinType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skin_type_id")
    private Long skinTypeId;

    @Column(name = "type_name", nullable = false, length = 50)
    private String typeName;
}
