package com.luna.skin.domain.cycle.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CycleInfoUpdateRequest {

    // "2일 이내" -> 2일
    @NotNull(message = "생리기간을 선택해주세요.")
    @Min(value = 2, message = "생리기간은 최소 1일보다 커야합니다.")
    @Max(value = 10, message = "생리기간은 최대 10일을 초과할 수 없습니다.")
    private Integer periodDuration;

    // "모르겠어요" -> 28일
    @NotNull(message = "생리 주기를 선택해주세요.")
    @Min(value = 20, message = "생리 주기는 최소 20일 이상이어야 합니다.")
    @Max(value = 40, message = "생리 주기는 최대 40일을 초과할 수 없습니다.")
    private Integer cycleLength;
}
