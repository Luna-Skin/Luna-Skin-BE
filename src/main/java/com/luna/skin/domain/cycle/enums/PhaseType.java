package com.luna.skin.domain.cycle.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public enum PhaseType {
    MENSTRUATION("생리 중엔 피부 장벽이 약해져 건조, 민감해지기 쉬워요"),
    FOLLICULAR("난포기엔 피부 회복 구간에스트로겐이 올라가며 피부가 점점 맑아지는 시기예요."),
    OVULATION("배란기엔 피부 컨디션 최상 구간배란기엔 피지·수분 밸런스가 가장 좋아요."),
    LUTEAL("황체기엔 프로게스테론 상승으로 피지 분비가 늘고 트러블이 생기기 쉬워요.");

    private final String comment;

}
