package com.luna.skin.domain.cycle.controller;

import com.luna.skin.domain.cycle.dto.response.CycleAndAnalysisDateResponse;
import com.luna.skin.domain.cycle.dto.response.CycleCommentResponse;
import com.luna.skin.domain.cycle.service.CycleService;
import com.luna.skin.global.response.BaseResponse;
import com.luna.skin.global.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cycle", description = "생리 주기 API")
@Validated
@RestController
@RequestMapping("/api/cycles")
@RequiredArgsConstructor
public class CycleController {

    private final CycleService cycleService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "월별 주기 캘린더 조회", description = "해당 월의 주기 단계와 AI 분석 완료 날짜를 반환합니다.")
    @GetMapping("/calendar")
    public ResponseEntity<BaseResponse<CycleAndAnalysisDateResponse>> getCalendar(
            @Parameter(description = "연도 (예: 2026)") @RequestParam @Min(2000) @Max(2100) int year,
            @Parameter(description = "월 (1~12)") @RequestParam @Min(1) @Max(12) int month
    ) {

        CycleAndAnalysisDateResponse cyclePhaseAtMonth = cycleService
                .getCyclePhaseAtMonth(currentUserProvider.getCurrentUserId(), year, month);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(cyclePhaseAtMonth));
    }

    @Operation(summary = "주기 단계별 코멘트 조회", description = "오늘 날짜에 해당하는 사용자의 주기 단계 별 코멘트를 조회합니다.")
    @GetMapping("/comment")
    public ResponseEntity<BaseResponse<CycleCommentResponse>> getCycleComment() {

        CycleCommentResponse cycleComment = cycleService
                .getCycleComment(currentUserProvider.getCurrentUserId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(cycleComment));
    }



}
