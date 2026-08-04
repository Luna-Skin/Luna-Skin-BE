package com.luna.skin.domain.analysis.entity;

import com.luna.skin.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "detailed_skin_analysis")
@Getter
@NoArgsConstructor
public class DetailedSkinAnalysis extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_analysis_id")
    private Long detailAnalysisId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false, unique = true)
    private AiAnalysis aiAnalysis;

    @Column(name = "sebum_level")
    private Integer sebumLevel;

    @Column(name = "redness")
    private Integer redness;

    @Column(name = "hydration_level")
    private Integer hydrationLevel;

    @Column(name = "pore_condition")
    private Integer poreCondition;

    @Column(name = "skin_elasticity")
    private Integer skinElasticity;
}
