package com.luna.skin.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_skin_concern")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
public class UserSkinConcern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_skin_concern_id")
    private Long userSkinConcernId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skin_concern_id", nullable = false)
    private SkinConcern skinConcern;

    public static UserSkinConcern of(User user, SkinConcern skinConcern) {
        return UserSkinConcern.builder()
                .user(user)
                .skinConcern(skinConcern)
                .build();
    }
}
