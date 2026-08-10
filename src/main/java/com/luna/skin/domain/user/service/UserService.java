package com.luna.skin.domain.user.service;

import com.luna.skin.domain.user.dto.response.UserSkinInfoResponse;
import com.luna.skin.domain.user.entity.SkinConcern;
import com.luna.skin.domain.user.entity.SkinType;
import com.luna.skin.domain.user.entity.User;
import com.luna.skin.domain.user.exception.UserErrorCode;
import com.luna.skin.domain.user.repository.SkinConcernRepository;
import com.luna.skin.domain.user.repository.SkinTypeRepository;
import com.luna.skin.domain.user.repository.UserRepository;
import com.luna.skin.domain.user.repository.UserSkinConcernRepository;
import com.luna.skin.domain.user.repository.UserSkinTypeRepository;
import com.luna.skin.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final SkinTypeRepository skinTypeRepository;
    private final SkinConcernRepository skinConcernRepository;
    private final UserSkinConcernRepository userSkinConcernRepository;
    private final UserSkinTypeRepository userSkinTypeRepository;

    public UserSkinInfoResponse getSkinInfo(Long currentUserId) {

        log.info("[피부 정보 조회] currentUserId = {}", currentUserId);

        userRepository.findById(currentUserId)
                .orElseThrow(() -> {
                    log.warn("[피부 정보 조회] 유저를 찾을 수 없습니다. currentUserId = {}", currentUserId);
                    return new CustomException(UserErrorCode.USER_NOT_FOUND);
                });

        // 전체 목록
        List<SkinType> allTypes = skinTypeRepository.findAll();
        List<SkinConcern> allConcerns = skinConcernRepository.findAll();

        // 사용자가 선택한 항목 ID Set
        Set<Long> selectedTypeIds = userSkinTypeRepository.findBySkinType(currentUserId)
                .stream().map(SkinType::getSkinTypeId).collect(Collectors.toSet());

        Set<Long> selectedConcernIds = userSkinConcernRepository.findBySkinConcern(currentUserId)
                .stream().map(SkinConcern::getSkinConcernId).collect(Collectors.toSet());

        return UserSkinInfoResponse.of(allTypes, selectedTypeIds, allConcerns, selectedConcernIds);
    }
}
