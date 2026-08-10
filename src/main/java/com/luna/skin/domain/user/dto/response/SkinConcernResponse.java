package com.luna.skin.domain.user.dto.response;

import com.luna.skin.domain.user.entity.SkinConcern;
import com.luna.skin.domain.user.entity.SkinType;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
public class SkinConcernResponse {

    private Long skinConcernId;
    private String concernName;
    private Boolean isSelected;

    public static SkinConcernResponse of(SkinConcern skinConcern, boolean isSelected) {
        return SkinConcernResponse.builder()
                .skinConcernId(skinConcern.getSkinConcernId())
                .concernName(skinConcern.getConcernName())
                .isSelected(isSelected)
                .build();
    }
}
