package com.luna.skin.domain.cycle.entity;

import com.luna.skin.domain.user.entity.User;
import com.luna.skin.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "menstruation_cycle")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenstruationCycle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menstruation_cycle_id")
    private Long menstruationCycleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "cycle_start_date")
    private LocalDate cycleStartDate;

    @Column(name = "cycle_end_date")
    private LocalDate cycleEndDate;

    @Column(name = "period_duration")
    @Builder.Default
    private Integer periodDuration = 4;

    @Column(name = "predicted_cycle_length")
    @Builder.Default
    private Integer predictedCycleLength = 28;

    // 시작일 갱신으로 인한 생리 기간 재 계산
    public void updateStartDate(LocalDate newStartDate) {
        // 기존 생리 종료일 보존
        LocalDate existingMenstruationEnd = this.cycleStartDate
                .plusDays(this.periodDuration - 1);

        this.cycleStartDate = newStartDate;
        // 새 시작일 ~ 기존 종료일로 periodDuration 재계산
        this.periodDuration = (int) ChronoUnit.DAYS.between(newStartDate,
                existingMenstruationEnd) + 1;
    }

    // 생리 종료일 갱신으로 인한 생리 기간 재 계산
    public void updateEndDate(LocalDate menstruationEndDate) {
        long daysBetween = ChronoUnit.DAYS.between(this.cycleStartDate,
                menstruationEndDate);
        this.periodDuration = (int) daysBetween + 1;
    }

    // 실제 주기 종료 시 갱신 (종료일, 실제 주기 길이)
    public void updateActualCycle(LocalDate endDate, int actualCycleLength) {
        this.cycleEndDate = endDate;
        this.predictedCycleLength = actualCycleLength;
    }

    // 생성 매서드
    public static MenstruationCycle of(User user, LocalDate cycleStartDate, LocalDate cycleEndDate, Integer predictedCycleLength, Integer periodDuration) {
        return MenstruationCycle.builder()
                .user(user)
                .cycleStartDate(cycleStartDate)
                .cycleEndDate(cycleEndDate)
                .predictedCycleLength(predictedCycleLength)
                .periodDuration(periodDuration)
                .build();
    }

}
