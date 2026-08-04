package com.luna.skin.domain.user.repository;

import com.luna.skin.domain.user.entity.UserSkinType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserSkinTypeRepository extends JpaRepository<UserSkinType, Long> {

    List<UserSkinType> findByUserUserId(Long userId);
}
