package com.luna.skin.domain.cycle.dto.response;

import com.luna.skin.domain.user.entity.User;
import lombok.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
public class CycleInfoResponse {

    private Integer defaultPeriodDuration;
    private Integer defaultCycleLength;

    public static CycleInfoResponse from(User user) {
        return CycleInfoResponse.builder()
                .defaultPeriodDuration(user.getDefaultPeriodDuration())
                .defaultCycleLength(user.getDefaultCycleLength())
                .build();
    }
}
