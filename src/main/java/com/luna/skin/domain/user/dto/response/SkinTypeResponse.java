package com.luna.skin.domain.user.dto.response;

import com.luna.skin.domain.user.entity.SkinType;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
public class SkinTypeResponse {

    private Long skinTypeId;
    private String typeName;
    private Boolean isSelected;

    public static SkinTypeResponse of(SkinType skinType, boolean isSelected) {
        return SkinTypeResponse.builder()
                .skinTypeId(skinType.getSkinTypeId())
                .typeName(skinType.getTypeName())
                .isSelected(isSelected)
                .build();
    }
}
