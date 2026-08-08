package com.luna.skin.domain.skin.entity;

import com.luna.skin.domain.skin.enums.ExerciseTime;
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

    @Column(name = "sleep_time")
    private Integer sleepTime;

    @Column(name = "water_intake")
    private Integer waterIntake;

    @Enumerated(EnumType.STRING)
    @Column(name = "diet_type")
    private String dietType; // "DAIRY,CAFFEINE,SPICY_FOOD" 형태로 저장

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_time")
    private ExerciseTime exerciseTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "skin_status")
    private SkinStatus skinStatus;
}
