package com.luna.skin.domain.analysis.controller;

import com.luna.skin.domain.analysis.dto.request.SkinAnalysisRequest;
import com.luna.skin.domain.analysis.dto.response.HomeSkinStatusResponse;
import com.luna.skin.domain.analysis.dto.response.ImageUploadResponse;
import com.luna.skin.domain.analysis.dto.response.SkinAnalysisResponse;
import com.luna.skin.domain.analysis.dto.response.SkinCompareResponse;
import com.luna.skin.domain.analysis.service.AnalysisService;
import com.luna.skin.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.luna.skin.global.security.CurrentUserProvider;

@Tag(name = "Analysis", description = "AI 분석 API")
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

  private final AnalysisService analysisService;
  private final CurrentUserProvider currentUserProvider;

  @Operation(summary = "피부 사진 업로드", description = "정면(필수), 왼쪽/오른쪽 측면(선택) 사진을 업로드하고 URL을 반환하는 API")
  @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BaseResponse<ImageUploadResponse>> uploadImages(
      @Parameter(description = "정면 사진 (필수)") @RequestPart("image") MultipartFile image,
      @Parameter(description = "왼쪽 측면 사진 (선택)") @RequestPart(value = "leftImage", required = false) MultipartFile leftImage,
      @Parameter(description = "오른쪽 측면 사진 (선택)") @RequestPart(value = "rightImage", required = false) MultipartFile rightImage) {
    Long userId = currentUserProvider.getCurrentUserId();
    return ResponseEntity.ok(BaseResponse.success(analysisService.uploadImages(userId, image, leftImage, rightImage)));
  }

  @Operation(summary = "일별 피부 분석", description = "오늘의 생활 기록과 사진으로 AI 피부 분석하는 API")
  @PostMapping("/{date}")
  public ResponseEntity<BaseResponse<SkinAnalysisResponse>> analyze(
      @PathVariable String date,
      @Valid @RequestBody SkinAnalysisRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(BaseResponse.success("COMMON_201", "요청에 성공했습니다.", analysisService.analyze(
            currentUserProvider.getCurrentUserId(),
            LocalDate.parse(date),
            request)));
  }

  @Operation(summary = "일별 피부 분석 조회", description = "특정 날짜의 피부 분석 기록을 조회하는 API")
  @GetMapping("/{date}")
  public ResponseEntity<BaseResponse<SkinAnalysisResponse>> getAnalysisByDate(
      @PathVariable String date) {
    return ResponseEntity.ok(BaseResponse.success(
        analysisService.getAnalysisByDate(
            currentUserProvider.getCurrentUserId(),
            LocalDate.parse(date))));
  }

  @Operation(summary = "홈 화면 오늘의 피부 상태", description = "오늘 피부 기록 summary를 반환하는 API")
  @GetMapping("/today")
  public ResponseEntity<BaseResponse<HomeSkinStatusResponse>> getTodaySkinStatus() {
    return ResponseEntity.ok(BaseResponse.success(
        analysisService.getTodaySkinStatus(currentUserProvider.getCurrentUserId())));
  }

  @Operation(summary = "피부 기록 비교", description = "두 날짜의 피부 분석 결과를 비교하는 API")
  @GetMapping("/compare")
  public ResponseEntity<BaseResponse<SkinCompareResponse>> compare(
      @Parameter(description = "기준 날짜", example = "2026-08-01") @RequestParam("dateA") LocalDate dateA,
      @Parameter(description = "비교 날짜", example = "2026-08-07") @RequestParam("dateB") LocalDate dateB) {
    Long userId = currentUserProvider.getCurrentUserId();
    return ResponseEntity.ok(BaseResponse.success(analysisService.compare(userId, dateA, dateB)));
  }
}
