package com.luna.skin.domain.analysis.entity;

import com.luna.skin.domain.skin.entity.TodaySkin;
import com.luna.skin.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_analysis")
@Getter
@NoArgsConstructor
public class AiAnalysis extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long analysisId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "today_skin_id", nullable = false, unique = true)
    private TodaySkin todaySkin;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "ai_comment", columnDefinition = "TEXT")
    private String aiComment;
}
