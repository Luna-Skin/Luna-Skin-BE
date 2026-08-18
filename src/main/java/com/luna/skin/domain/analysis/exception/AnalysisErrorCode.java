package com.luna.skin.domain.analysis.exception;

import com.luna.skin.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AnalysisErrorCode implements ErrorCode {
  INVALID_FILE_TYPE("ANALYSIS_400", "지원하지 않는 파일 형식입니다. (jpg, jpeg, png)", HttpStatus.BAD_REQUEST),
  EMPTY_FILE("ANALYSIS_400_EMPTY", "파일이 비어있습니다.", HttpStatus.BAD_REQUEST),
  ANALYSIS_NOT_FOUND("ANALYSIS_404", "분석 기록을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  CYCLE_NOT_FOUND("ANALYSIS_404_CYCLE", "생리 주기 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  GPT_ANALYSIS_FAILED("ANALYSIS_500", "AI 분석 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
