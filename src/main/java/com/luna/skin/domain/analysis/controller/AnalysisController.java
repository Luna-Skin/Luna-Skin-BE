package com.luna.skin.domain.analysis.controller;

import com.luna.skin.domain.analysis.dto.request.SkinAnalysisRequest;
import com.luna.skin.domain.analysis.dto.response.ImageUploadResponse;
import com.luna.skin.domain.analysis.dto.response.SkinAnalysisResponse;
import com.luna.skin.domain.analysis.service.AnalysisService;
import com.luna.skin.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.luna.skin.global.security.CurrentUserProvider;

@Tag(name = "Analysis", description = "AI 분석 API")
@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class AnalysisController {

  private final AnalysisService analysisService;
  private final CurrentUserProvider currentUserProvider;

  @Operation(summary = "피부 사진 업로드", description = "피부 사진을 업로드하고 image_url을 반환하는 API")
  @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BaseResponse<ImageUploadResponse>> uploadImage(
      @RequestPart("image") MultipartFile image){
    Long userId = currentUserProvider.getCurrentUserId();
    return ResponseEntity.ok(BaseResponse.success(analysisService.uploadImage(userId, image)));
  }

  @Operation(summary = "일별 피부 분석", description = "생활습관 데이터와 사진으로 AI 피부 분석")
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
}
