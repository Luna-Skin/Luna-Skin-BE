package com.luna.skin.domain.user.controller;

import com.luna.skin.domain.user.dto.response.UserSkinInfoResponse;
import com.luna.skin.domain.user.service.UserService;
import com.luna.skin.global.response.BaseResponse;
import com.luna.skin.global.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "피부 정보 조회", description = "사용자의 피부타입과 피부 고민을 조회합니다.")
    @GetMapping("/skin")
    public ResponseEntity<BaseResponse<UserSkinInfoResponse>> getSkinInfo() {
        UserSkinInfoResponse skinInfo = userService
                .getSkinInfo(currentUserProvider.getCurrentUserId());

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(skinInfo));
    }
}
