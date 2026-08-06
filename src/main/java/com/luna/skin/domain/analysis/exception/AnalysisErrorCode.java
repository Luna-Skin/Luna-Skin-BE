package com.luna.skin.domain.analysis.exception;

import com.luna.skin.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AnalysisErrorCode implements ErrorCode {
  INVALID_FILE_TYPE("ANALYSIS_400", "지원하지 않는 파일 형식입니다. (jpg, jpeg, png)", HttpStatus.BAD_REQUEST),
  EMPTY_FILE("ANALYSIS_400", "파일이 비어있습니다.", HttpStatus.BAD_REQUEST);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
