package com.luna.skin.domain.user.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
public class UserSkinProfileResponse {

    private String name;
    private String skinType;
    private List<String> selectedSkinConcerns;

    public static UserSkinProfileResponse from(String name,
                                               String skinType,
                                               List<String> selectedSkinConcerns) {
        return UserSkinProfileResponse.builder()
                .name(name)
                .skinType(skinType)
                .selectedSkinConcerns(selectedSkinConcerns)
                .build();
    }
}
