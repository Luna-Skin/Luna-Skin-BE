package com.luna.skin.domain.analysis.service;

import com.luna.skin.domain.analysis.dto.response.ImageUploadResponse;
import com.luna.skin.domain.analysis.exception.AnalysisErrorCode;
import com.luna.skin.global.exception.CustomException;
import com.luna.skin.global.storage.ImageStorageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

  private final ImageStorageService imageStorageService;

  public ImageUploadResponse uploadImage(MultipartFile image) {
    validateImageFile(image);

    String imageUrl = imageStorageService.store(image, "analysis");
    return ImageUploadResponse.builder()
        .imageUrl(imageUrl)
        .build();
  }

  private void validateImageFile(MultipartFile image) {
    if(image == null || image.isEmpty()) {
      throw new CustomException(AnalysisErrorCode.EMPTY_FILE);
    }

    String originalFilename = image.getOriginalFilename();
    if(originalFilename == null || originalFilename.isEmpty()) {
      throw new CustomException(AnalysisErrorCode.INVALID_FILE_TYPE);
    }

    String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
    if (!List.of("jpg", "jpeg", "png").contains(ext)) {
      throw new CustomException(AnalysisErrorCode.INVALID_FILE_TYPE);
    }


  }
}
