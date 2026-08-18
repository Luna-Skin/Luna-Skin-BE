package com.luna.skin.domain.analysis.entity;

import com.luna.skin.domain.analysis.enums.SkinStatusLabel;
import com.luna.skin.domain.skin.entity.TodaySkin;
import com.luna.skin.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_analysis")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Enumerated(EnumType.STRING)
    @Column(name = "skin_status_label", length = 20)
    private SkinStatusLabel skinStatusLabel;

    @Column(name = "phase_comment")
    private String phaseComment;

    @Column(name = "ai_comment", columnDefinition = "TEXT")
    private String aiComment;

    // 같은 날짜 재분석 시 기존 분석 결과를 덮어쓰기 위한 갱신
    public void update(Integer overallScore, SkinStatusLabel skinStatusLabel,
        String aiComment, String phaseComment) {
        this.overallScore = overallScore;
        this.skinStatusLabel = skinStatusLabel;
        this.aiComment = aiComment;
        this.phaseComment = phaseComment;
    }
}
