package com.luna.skin.domain.chat.controller;

import com.luna.skin.domain.chat.dto.request.CreateChatRoomRequest;
import com.luna.skin.domain.chat.dto.request.RenameChatRoomRequest;
import com.luna.skin.domain.chat.dto.response.ChatMessagePageResponse;
import com.luna.skin.domain.chat.dto.response.ChatRoomListPageResponse;
import com.luna.skin.domain.chat.dto.response.CreateChatRoomResponse;
import com.luna.skin.domain.chat.dto.response.RenameChatRoomResponse;
import com.luna.skin.domain.chat.service.ChatService;
import com.luna.skin.global.response.BaseResponse;
import com.luna.skin.global.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat", description = "AI 채팅방 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/rooms")
    @Operation(
            summary = "채팅방 목록 조회",
            description = "요청한 유저의 채팅방 목록을 페이지네이션으로 반환합니다. "
                    + "X-USER-ID 헤더에 유저 ID를 입력해 테스트하세요. 예: X-USER-ID: 1"
    )
    public ResponseEntity<BaseResponse<ChatRoomListPageResponse>> getAllChatRoomList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ChatRoomListPageResponse chatRoomList = chatService
                .getAllChatRoomList(currentUserProvider.getCurrentUserId(), page, size);

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

    @GetMapping("/rooms/{roomId}/messages")
    @Operation(
            summary = "대화 내역 조회",
            description = "본인 소유의 채팅방에 대한 AI_Chat_Message 목록을 페이지네이션으로 반환합니다(최신 메시지부터). "
                    + "본인 소유가 아니면 403이 반환됩니다. X-USER-ID 헤더에 유저 ID를 입력해 테스트하세요. 예: X-USER-ID: 1"
    )
    public ResponseEntity<BaseResponse<ChatMessagePageResponse>> getChatMessages(
            @Parameter(description = "대화 내역을 조회할 채팅방 식별자", example = "1")
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ChatMessagePageResponse messages = chatService
                .getChatMessages(currentUserProvider.getCurrentUserId(), roomId, page, size);

        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(messages));
    }

    @PatchMapping("/rooms/{roomId}")
    @Operation(
            summary = "채팅방 이름 변경",
            description = "본인 소유의 채팅방 제목을 변경합니다. 본인 소유가 아니면 403이 반환됩니다. "
                    + "X-USER-ID 헤더에 유저 ID를 입력해 테스트하세요. 예: X-USER-ID: 1"
    )
    public ResponseEntity<BaseResponse<RenameChatRoomResponse>> renameChatRoom(
            @Parameter(description = "이름을 변경할 채팅방 식별자", example = "1")
            @PathVariable Long roomId,
            @RequestBody @Valid RenameChatRoomRequest request
    ) {
        RenameChatRoomResponse response = chatService
                .renameChatRoom(currentUserProvider.getCurrentUserId(), roomId, request.getTitle());

        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(response));
    }

    @PostMapping("/rooms/analyses/{analysisId}")
    @Operation(
            summary = "분석 기록 기반 채팅방 조회/생성",
            description = "특정 투데이스킨 분석 기록에 대한 채팅방을 조회하거나, 없으면 새로 생성합니다. "
                    + "이미 해당 기록에 대한 채팅방이 있으면 그 방을 그대로 반환합니다(find-or-create). "
                    + "본인 소유의 분석 기록이 아니면 403이 반환됩니다. X-USER-ID 헤더에 유저 ID를 입력해 테스트하세요. 예: X-USER-ID: 1"
    )
    public ResponseEntity<BaseResponse<CreateChatRoomResponse>> findOrCreateChatRoomFromAnalysis(
            @Parameter(description = "질문할 대상 분석 기록 식별자", example = "1")
            @PathVariable Long analysisId
    ) {
        CreateChatRoomResponse response = chatService
                .findOrCreateChatRoomFromAnalysis(currentUserProvider.getCurrentUserId(), analysisId);

        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(response));
    }

}
