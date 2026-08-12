package com.luna.skin.domain.user.dto.response;

import com.luna.skin.domain.user.entity.User;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
public class UserResponse {

    private String name;
    private String email;
    private String profileImageUrl;
    private Boolean isSubscription;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .name(user.getName())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .isSubscription(user.isSubscription())
                .build();
    }

}
