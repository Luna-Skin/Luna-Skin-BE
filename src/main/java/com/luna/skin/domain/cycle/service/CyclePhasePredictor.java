package com.luna.skin.domain.cycle.service;

import com.luna.skin.domain.cycle.entity.CyclePhase;
import com.luna.skin.domain.cycle.entity.MenstruationCycle;
import com.luna.skin.domain.cycle.enums.PhaseType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static com.luna.skin.domain.cycle.enums.PhaseType.*;

// 주기가 없을 경우 오늘날짜에 해당하는 예측 단계를 조회
@Component
public class CyclePhasePredictor {

    public CyclePhase predict(MenstruationCycle lastCycle, LocalDate today) {


        PhaseType phaseType;

        int cycleLength = lastCycle.getUser().getDefaultCycleLength();
        int periodDuration = lastCycle.getUser().getDefaultPeriodDuration();
        int ovulationOffset = cycleLength / 2 - 1;

        int daysIntoCycle = (int) ChronoUnit.DAYS
                .between(lastCycle.getCycleStartDate(), today) % cycleLength;

        if (daysIntoCycle < periodDuration) phaseType = MENSTRUATION;
        else if (daysIntoCycle < ovulationOffset) phaseType = FOLLICULAR;
        else if (daysIntoCycle <= ovulationOffset + 2) phaseType = OVULATION;
        else phaseType = LUTEAL;

        return CyclePhase.builder()
                .phaseType(phaseType)
                .build();
    }
}
