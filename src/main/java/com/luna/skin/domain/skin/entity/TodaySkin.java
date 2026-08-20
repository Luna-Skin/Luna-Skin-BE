package com.luna.skin.domain.skin.entity;

import com.luna.skin.domain.skin.enums.SkinStatus;
import com.luna.skin.domain.user.entity.User;
import com.luna.skin.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "today_skin")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodaySkin extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "today_skin_id")
    private Long todaySkinId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "left_image_url")
    private String leftImageUrl;

    @Column(name = "right_image_url")
    private String rightImageUrl;

    @Column(name = "sleep_time")
    private Double sleepTime;

    @Column(name = "water_intake")
    private Double waterIntake;

    @Column(name = "diet_type")
    private String dietType; // "DAIRY,CAFFEINE,SPICY_FOOD" 형태로 저장

    @Column(name = "exercise_time", columnDefinition = "integer")
    private Integer exerciseTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "skin_status")
    private SkinStatus skinStatus;

    // 같은 날짜 재분석 시 기존 기록을 덮어쓰기 위한 갱신
    public void update(String imageUrl, String leftImageUrl, String rightImageUrl,
        Double sleepTime, Double waterIntake, String dietType, Integer exerciseTime,
        SkinStatus skinStatus) {
        this.imageUrl = imageUrl;
        this.leftImageUrl = leftImageUrl;
        this.rightImageUrl = rightImageUrl;
        this.sleepTime = sleepTime;
        this.waterIntake = waterIntake;
        this.dietType = dietType;
        this.exerciseTime = exerciseTime;
        this.skinStatus = skinStatus;
    }
}
