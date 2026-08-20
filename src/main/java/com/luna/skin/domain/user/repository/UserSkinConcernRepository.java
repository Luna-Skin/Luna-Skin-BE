package com.luna.skin.domain.user.repository;

import com.luna.skin.domain.user.entity.SkinConcern;
import com.luna.skin.domain.user.entity.UserSkinConcern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserSkinConcernRepository extends JpaRepository<UserSkinConcern, Long> {


    // 유저가 선택한 피부 고민을 조회
    @Query("select us.skinConcern from UserSkinConcern us " +
            "where us.user.userId = :currentUserId ")
    List<SkinConcern> findBySkinConcern(
            @Param("currentUserId") Long currentUserId
    );

    // 유저의 모든 피부 고민을 지움
    @Modifying(flushAutomatically = true)
    @Query("delete from UserSkinConcern us where us.user.userId = :userId")
    void deleteAllByUserUserId(@Param("userId") Long userId);
}
