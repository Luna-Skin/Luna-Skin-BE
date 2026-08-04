package com.luna.skin.domain.product.entity;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prod_recommend")
@Getter
@NoArgsConstructor
public class ProdRecommend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prod_recommend_id")
    private Long prodRecommendId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AiAnalysis aiAnalysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "match_percentage")
    private Integer matchPercentage;
}
