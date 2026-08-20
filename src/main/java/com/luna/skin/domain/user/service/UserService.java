package com.luna.skin.domain.user.service;

import com.luna.skin.domain.user.dto.request.UserSkinInfoUpdateRequest;
import com.luna.skin.domain.user.dto.response.UserResponse;
import com.luna.skin.domain.user.dto.response.UserSkinInfoResponse;
import com.luna.skin.domain.user.dto.response.UserSkinProfileResponse;
import com.luna.skin.domain.user.entity.SkinConcern;
import com.luna.skin.domain.user.entity.SkinType;
import com.luna.skin.domain.user.entity.User;
import com.luna.skin.domain.user.entity.UserSkinConcern;
import com.luna.skin.domain.user.entity.UserSkinType;
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


    public UserResponse getUserInfo(Long currentUserId) {

        log.warn("[내 프로필 조회] currnetUserId = {}", currentUserId);

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> {
                    log.warn("[내 프로필 조회] 사용자를 찾을 수 없습니다. currentUserId = {}", currentUserId);
                    return new CustomException(UserErrorCode.USER_NOT_FOUND);
                });

        return UserResponse.from(user);
    }

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
                .map(SkinType::getSkinTypeId)
                .map(Set::of)
                .orElse(Set.of());

        Set<Long> selectedConcernIds = userSkinConcernRepository.findBySkinConcern(currentUserId)
                .stream().map(SkinConcern::getSkinConcernId).collect(Collectors.toSet());

        return UserSkinInfoResponse.of(allTypes, selectedTypeIds, allConcerns, selectedConcernIds);
    }

    public UserSkinProfileResponse getSkinProfile(Long currentUserId) {

        log.info("[홈 헤더 조회] currentUserId = {}", currentUserId);

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> {
                    log.warn("[홈 헤더 조회] 사용자를 찾을 수 없습니다. userId = {}", currentUserId);
                    return new CustomException(UserErrorCode.USER_NOT_FOUND);
                });

        // 혹시 피부 타입 선택을 안했다면 null
        String skinType = userSkinTypeRepository.findBySkinType(currentUserId)
                .map(SkinType::getTypeName).orElse(null);

        List<String> selectedSkinConcerns = userSkinConcernRepository.findBySkinConcern(currentUserId)
                .stream().map(SkinConcern::getConcernName).toList();

        return UserSkinProfileResponse.from(user.getName(), skinType, selectedSkinConcerns);
    }

    @Transactional
    public void updateSkinInfo(Long currentUserId, UserSkinInfoUpdateRequest request) {

        log.info("[피부 정보 수정] currentUserId = {}", currentUserId);

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> {
                    log.warn("[피부 정보 수정] 유저를 찾을 수 없습니다. currentUserId = {}", currentUserId);
                    return new CustomException(UserErrorCode.USER_NOT_FOUND);
                });

        SkinType skinType = skinTypeRepository.findById(request.getSkinTypeId())
                .orElseThrow(() -> {
                    log.warn("[피부 정보 수정] 유효하지 않는 스킨타입입니다.");
                    return new CustomException(UserErrorCode.INVALID_SKIN_TYPE);
                });

        List<SkinConcern> skinConcerns = skinConcernRepository.findAllById(request.getSkinConcernIds());
        if (skinConcerns.size() != request.getSkinConcernIds().size()) {
            log.warn("[피부 정보 수정] 유효하지 않는 피부고민이 있습니다.");
            throw new CustomException(UserErrorCode.INVALID_SKIN_CONCERN);
        }

        // 피부 타입 삭제
        userSkinTypeRepository.deleteAllByUserUserId(currentUserId);
        // 피부 고민 삭제
        userSkinConcernRepository.deleteAllByUserUserId(currentUserId);

        // 다시 저장
        userSkinTypeRepository.save(UserSkinType.of(user, skinType));
        userSkinConcernRepository.saveAll(
                skinConcerns.stream().map(sc -> UserSkinConcern.of(user, sc)).toList());
    }
}
