package com.luna.skin.domain.user.repository;

import com.luna.skin.domain.user.entity.UserSkinConcern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserSkinConcernRepository extends JpaRepository<UserSkinConcern, Long> {

    List<UserSkinConcern> findByUserUserId(Long userId);
}
