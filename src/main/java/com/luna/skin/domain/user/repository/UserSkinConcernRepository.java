package com.luna.skin.domain.user.repository;

import com.luna.skin.domain.user.entity.SkinConcern;
import com.luna.skin.domain.user.entity.UserSkinConcern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserSkinConcernRepository extends JpaRepository<UserSkinConcern, Long> {


    @Query("select us.skinConcern from UserSkinConcern us " +
            "where us.user.userId = :currentUserId ")
    List<SkinConcern> findBySkinConcern(
            @Param("currentUserId") Long currentUserId
    );
}
