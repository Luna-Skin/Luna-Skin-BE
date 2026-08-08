package com.luna.skin.domain.routine.controller;

import com.luna.skin.domain.routine.dto.response.AiDailyRoutineResponse;
import com.luna.skin.domain.routine.service.RoutineService;
import com.luna.skin.global.response.BaseResponse;
import com.luna.skin.global.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Routine", description = "AI 추천 루틴 API")
@RestController
@RequestMapping("/api/routines")
@RequiredArgsConstructor
public class RoutineController {

    private final RoutineService routineService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "오늘의 루틴 조회", description = "주기 단계와 최근 피부 분석을 바탕으로 AI가 추천한 오늘의 루틴을 반환합니다.")
    @GetMapping("/routine")
    public ResponseEntity<BaseResponse<AiDailyRoutineResponse>> getTodayRoutine() {

        AiDailyRoutineResponse DailyRoutineResponse = routineService
                .generateDailyRoutine(currentUserProvider.getCurrentUserId(), LocalDate.now());

        return ResponseEntity.ok(BaseResponse.success(DailyRoutineResponse));
    }
}
