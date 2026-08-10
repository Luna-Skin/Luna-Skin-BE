package com.luna.skin.domain.routine.entity;

import com.luna.skin.domain.routine.enums.RoutineCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "routine_content")
@Getter
@NoArgsConstructor
public class RoutineContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_id")
    private Long contentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private RoutineCategory category;

    @Column(name = "content", nullable = false)
    private String content;
}
