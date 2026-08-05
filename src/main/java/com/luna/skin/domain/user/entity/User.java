package com.luna.skin.domain.user.entity;

import com.luna.skin.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", nullable = false, unique = true, length = 50)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "subscription", nullable = false)
    private boolean subscription = false;

    @Column(name = "default_period_duration", nullable = false)
    private Integer defaultPeriodDuration = 4;

    @Column(name = "default_cycle_length", nullable = false)
    private Integer defaultCycleLength = 28;

    public void updateCycleSettings(int periodDuration, int cycleLength) {
        this.defaultPeriodDuration = periodDuration;
        this.defaultCycleLength = cycleLength;
    }
}
