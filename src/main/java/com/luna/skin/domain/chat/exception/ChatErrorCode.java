package com.luna.skin.domain.chat.exception;

import com.luna.skin.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {
    CHAT_ROOM_NOT_FOUND("CHAT_ROOM_404", "해당 채팅방을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CHAT_ROOM_ACCESS_DENIED("CHAT_ROOM_403", "본인의 채팅방만 접근할 수 있습니다.", HttpStatus.FORBIDDEN),
    CHAT_USER_NOT_FOUND("CHAT_USER_404", "존재하지 않는 유저입니다.", HttpStatus.NOT_FOUND),
    CHAT_ANALYSIS_NOT_FOUND("CHAT_ANALYSIS_404", "존재하지 않는 분석입니다.", HttpStatus.NOT_FOUND),
    CHAT_ANALYSIS_ACCESS_DENIED("CHAT_ANALYSIS_403", "본인의 분석 기록만 조회할 수 있습니다.", HttpStatus.FORBIDDEN),
    AI_RESPONSE_FAILED("CHAT_AI_502", "AI 응답을 받아오지 못했습니다.", HttpStatus.BAD_GATEWAY),
    CHAT_FILE_EMPTY("CHAT_FILE_400", "파일이 비어있습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
