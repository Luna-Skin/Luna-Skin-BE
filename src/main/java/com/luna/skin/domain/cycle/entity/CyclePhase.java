package com.luna.skin.domain.cycle.entity;

import com.luna.skin.domain.cycle.enums.PhaseType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "cycle_phase")
@Getter
@NoArgsConstructor
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
}
