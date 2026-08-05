package com.luna.skin.domain.cycle.entity;

import com.luna.skin.domain.cycle.enums.PhaseType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "cycle_phase")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CyclePhase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cycle_phase_id")
    private Long cyclePhaseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menstruation_cycle_id", nullable = false)
    private MenstruationCycle menstruationCycle;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase_type", nullable = false)
    private PhaseType phaseType;


    // 주기 별 주기 단계 계산
    public static List<CyclePhase> of(MenstruationCycle cycle) {

        LocalDate start = cycle.getCycleStartDate();
        int periodDuration = cycle.getPeriodDuration();
        int cycleLength = cycle.getPredictedCycleLength();

        // 해당 주기의 배란 시작일
        LocalDate ovulationStart = start.plusDays((long) cycleLength / 2 - 1);

        return List.of(
                CyclePhase.builder()
                        .menstruationCycle(cycle)
                        .phaseType(PhaseType.MENSTRUATION)
                        .startDate(start)
                        .endDate(start.plusDays(periodDuration - 1))
                        .build(),
                CyclePhase.builder()
                        .menstruationCycle(cycle)
                        .phaseType(PhaseType.FOLLICULAR)
                        .startDate(start.plusDays(periodDuration))
                        .endDate(ovulationStart.minusDays(1))
                        .build(),
                CyclePhase.builder()
                        .menstruationCycle(cycle)
                        .phaseType(PhaseType.OVULATION)
                        .startDate(ovulationStart)
                        .endDate(ovulationStart.plusDays(2))
                        .build(),
                CyclePhase.builder()
                        .menstruationCycle(cycle)
                        .phaseType(PhaseType.LUTEAL)
                        .startDate(ovulationStart.plusDays(3))
                        .endDate(start.plusDays(cycleLength - 1))
                        .build()
        );
    }
}
