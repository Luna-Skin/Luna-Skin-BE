package com.luna.skin.domain.chat.dto.response;

import com.luna.skin.domain.chat.entity.AiChatRoom;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatRoomListPageResponse {

    private List<ChatRoomListResponse> chatRooms;
    private boolean hasNext;

    public static ChatRoomListPageResponse from(Slice<AiChatRoom> slice) {
        List<ChatRoomListResponse> chatRooms = slice.getContent().stream()
                .map(ChatRoomListResponse::from)
                .toList();

        return new ChatRoomListPageResponse(chatRooms, slice.hasNext());
    }
}
