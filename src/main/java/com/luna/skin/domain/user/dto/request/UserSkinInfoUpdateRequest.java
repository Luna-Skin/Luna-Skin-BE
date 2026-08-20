package com.luna.skin.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class UserSkinInfoUpdateRequest {

    @NotNull(message = "피부 타입을 선택해주세요.")
    private Long skinTypeId;

    @NotNull(message = "피부 고민을 선택해주세요.")
    private List<Long> skinConcernIds;
}
