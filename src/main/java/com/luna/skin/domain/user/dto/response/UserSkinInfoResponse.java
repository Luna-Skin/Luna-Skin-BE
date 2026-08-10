package com.luna.skin.domain.user.dto.response;

import com.luna.skin.domain.user.entity.SkinConcern;
import com.luna.skin.domain.user.entity.SkinType;
import lombok.*;

import java.util.List;
import java.util.Set;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
public class UserSkinInfoResponse {

    private List<SkinTypeResponse> skinTypes;
    private List<SkinConcernResponse> skinConcerns;

    public static UserSkinInfoResponse of(
            List<SkinType> allTypes, Set<Long> selectedTypeIds,
            List<SkinConcern> allConcerns, Set<Long> selectedConcernIds) {

        List<SkinTypeResponse> types = allTypes.stream()
                .map(st -> SkinTypeResponse.of(st, selectedTypeIds.contains(st.getSkinTypeId())))
                .toList();

        List<SkinConcernResponse> concerns = allConcerns.stream()
                .map(sc -> SkinConcernResponse.of(sc, selectedConcernIds.contains(sc.getSkinConcernId())))
                .toList();

        return UserSkinInfoResponse.builder()
                .skinTypes(types)
                .skinConcerns(concerns)
                .build();
    }
}
