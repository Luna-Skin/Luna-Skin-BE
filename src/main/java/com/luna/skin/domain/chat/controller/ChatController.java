package com.luna.skin.domain.chat.controller;

import com.luna.skin.domain.chat.dto.response.ChatRoomListResponse;
import com.luna.skin.domain.chat.service.ChatService;
import com.luna.skin.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/rooms")
    @Operation(
            summary = "채팅방 목록 조회 컨트롤러입니다.",
            description = "채팅방 식별자, 채팅방 제목, 채팅방 생성 시간을 반환합니다."
    )
    public ResponseEntity<BaseResponse<List<ChatRoomListResponse>>> getAllChatRoomList(
            @RequestParam Long userId
    ) {

        List<ChatRoomListResponse> chatRoomList =
                chatService.getAllChatRoomList(userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(chatRoomList));
    }

}
