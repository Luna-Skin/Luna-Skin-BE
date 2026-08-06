package com.luna.skin.domain.analysis.controller;

import com.luna.skin.domain.analysis.dto.response.ImageUploadResponse;
import com.luna.skin.domain.analysis.service.AnalysisService;
import com.luna.skin.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Analysis", description = "AI 분석 API")
@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class AnalysisController {

  private final AnalysisService analysisService;

  @Operation(summary = "피부 사진 업로드", description = "피부 사진을 업로드하고 image_url을 반환하는 API")
  @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BaseResponse<ImageUploadResponse>> uploadImage(
      @RequestPart("image") MultipartFile image){
    return ResponseEntity.ok(BaseResponse.success(analysisService.uploadImage(image)));
  }
}
