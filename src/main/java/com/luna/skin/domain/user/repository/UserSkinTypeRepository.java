package com.luna.skin.domain.user.repository;

import com.luna.skin.domain.user.entity.SkinConcern;
import com.luna.skin.domain.user.entity.SkinType;
import com.luna.skin.domain.user.entity.UserSkinType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserSkinTypeRepository extends JpaRepository<UserSkinType, Long> {


    @Query("select ust.skinType from UserSkinType ust " +
            "where ust.user.userId = :currentUserId")
    List<SkinType> findBySkinType(
            @Param("currentUserId") Long currentUserId
    );
}
