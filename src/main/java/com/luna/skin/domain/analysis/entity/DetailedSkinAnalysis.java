package com.luna.skin.domain.analysis.entity;

import com.luna.skin.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "detailed_skin_analysis")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetailedSkinAnalysis extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_analysis_id")
    private Long detailAnalysisId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false, unique = true)
    private AiAnalysis aiAnalysis;

    @Column(name = "sebum")
    private Integer sebum;

    @Column(name = "trouble")
    private Integer trouble;

    @Column(name = "moisture")
    private Integer moisture;

    @Column(name = "dullness")
    private Integer dullness;

    @Column(name = "elasticity")
    private Integer elasticity;

    // 같은 날짜 재분석 시 기존 상세 지표를 덮어쓰기 위한 갱신
    public void update(Integer trouble, Integer sebum, Integer dullness,
        Integer moisture, Integer elasticity) {
        this.trouble = trouble;
        this.sebum = sebum;
        this.dullness = dullness;
        this.moisture = moisture;
        this.elasticity = elasticity;
    }
}
