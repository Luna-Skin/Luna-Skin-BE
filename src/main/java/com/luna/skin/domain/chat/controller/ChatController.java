package com.luna.skin.domain.chat.controller;

import com.luna.skin.domain.chat.dto.request.CreateChatRoomRequest;
import com.luna.skin.domain.chat.dto.response.ChatRoomListResponse;
import com.luna.skin.domain.chat.dto.response.CreateChatRoomResponse;
import com.luna.skin.domain.chat.service.ChatService;
import com.luna.skin.global.response.BaseResponse;
import com.luna.skin.global.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chat", description = "AI 채팅방 API")
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/rooms")
    @Operation(
            summary = "채팅방 목록 조회",
            description = "요청한 유저의 채팅방 식별자, 채팅방 제목, 채팅방 생성 시간을 반환합니다. "
                    + "X-USER-ID 헤더에 유저 ID를 입력해 테스트하세요. 예: X-USER-ID: 1"
    )
    public ResponseEntity<BaseResponse<List<ChatRoomListResponse>>> getAllChatRoomList() {

        List<ChatRoomListResponse> chatRoomList =
                chatService.getAllChatRoomList(currentUserProvider.getCurrentUserId());

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(chatRoomList));
    }

    @PostMapping("/rooms")
    @Operation(
            summary = "채팅방 생성",
            description = "채팅방을 생성하고 채팅방 식별자, 채팅방 제목, 채팅방 생성 시간을 반환합니다. "
                    + "X-USER-ID 헤더에 유저 ID를 입력해 테스트하세요. 예: X-USER-ID: 1"
    )
    public ResponseEntity<BaseResponse<CreateChatRoomResponse>> createChatRoom(
            @RequestBody CreateChatRoomRequest createChatRoomRequest
    ) {
        CreateChatRoomResponse createChatRoom = chatService.createChatRoom(
                currentUserProvider.getCurrentUserId(), createChatRoomRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(createChatRoom));
    }

    @DeleteMapping("/rooms/{roomId}")
    @Operation(
            summary = "채팅방 삭제",
            description = "본인 소유의 채팅방을 삭제합니다. 본인 소유가 아니면 403이 반환됩니다. "
                    + "X-USER-ID 헤더에 유저 ID를 입력해 테스트하세요. 예: X-USER-ID: 1"
    )
    public ResponseEntity<BaseResponse<Boolean>> deleteChatRoom(
            @Parameter(description = "삭제할 채팅방 식별자", example = "1")
            @PathVariable Long roomId
    ) {
        chatService.deleteChatRoom(currentUserProvider.getCurrentUserId(), roomId);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(true));
    }

}
