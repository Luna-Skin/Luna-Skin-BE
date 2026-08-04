package com.luna.skin.domain.cycle.entity;

import com.luna.skin.domain.user.entity.User;
import com.luna.skin.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "menstruation_cycle")
@Getter
@NoArgsConstructor
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
    private Integer periodDuration = 4;

    @Column(name = "predicted_cycle_length")
    private Integer predictedCycleLength = 28;
}
