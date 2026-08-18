package com.luna.skin.domain.chat.service;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.analysis.repository.DetailedSkinAnalysisRepository;
import com.luna.skin.domain.chat.dto.request.CreateChatRoomRequest;
import com.luna.skin.domain.chat.dto.response.ChatErrorResponse;
import com.luna.skin.domain.chat.dto.response.ChatMessagePageResponse;
import com.luna.skin.domain.chat.dto.response.ChatMessageResponse;
import com.luna.skin.domain.chat.dto.response.ChatRoomListPageResponse;
import com.luna.skin.domain.chat.dto.response.CreateChatRoomResponse;
import com.luna.skin.domain.chat.dto.response.RenameChatRoomResponse;
import com.luna.skin.domain.chat.entity.AiChatMessage;
import com.luna.skin.domain.chat.entity.AiChatRoom;
import com.luna.skin.domain.chat.enums.MessageRole;
import com.luna.skin.domain.chat.enums.MessageType;
import com.luna.skin.domain.chat.exception.ChatErrorCode;
import com.luna.skin.domain.chat.repository.AiChatMessageRepository;
import com.luna.skin.domain.chat.repository.AiChatRoomRepository;
import com.luna.skin.domain.user.entity.User;
import com.luna.skin.domain.user.repository.UserRepository;
import com.luna.skin.global.exception.CustomException;
import com.luna.skin.global.storage.ImageStorageService;
import com.luna.skin.infra.openai.OpenAiChatClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatServiceImpl implements ChatService {

    private static final String CHAT_ROOM_TOPIC_PREFIX = "/topic/chat/";
    private static final DateTimeFormatter TITLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 M월 d일");
    private static final String GENERAL_WELCOME_MESSAGE = "안녕하세요! 끼끼의 피부상담소입니다.\n무엇이 궁금하신가요?";

    private final AiChatRoomRepository aiChatRoomRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final UserRepository userRepository;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final DetailedSkinAnalysisRepository detailedSkinAnalysisRepository;
    private final OpenAiChatClient openAiChatClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final ImageStorageService imageStorageService;

    @Override
    @Transactional(readOnly = true)
    public ChatRoomListPageResponse getAllChatRoomList(Long userId, int page, int size) {

        log.info("[ChatService] 채팅방 목록 조회 - 시작: userId={}", userId);

        Slice<AiChatRoom> chatRooms = aiChatRoomRepository
                .findByUser_UserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));

        log.info("[ChatService] 채팅방 목록 조회 - 완료: 조회 결과={}건, hasNext={}",
                chatRooms.getContent().size(), chatRooms.hasNext());

        return ChatRoomListPageResponse.from(chatRooms);
    }

    @Override
    public CreateChatRoomResponse createChatRoom(Long userId, CreateChatRoomRequest createChatRoomRequest) {

        log.info("[ChatService] 채팅방 생성 - 시작: title:{}", createChatRoomRequest.getTitle());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[ChatService] 채팅방 생성 - 에러: 존재하지 않는 유저입니다. id={}", userId);
                    return new CustomException(ChatErrorCode.CHAT_USER_NOT_FOUND);
                });

        Long aiAnalysisId = createChatRoomRequest.getAiAnalysis();
        AiAnalysis aiAnalysis = null;

        if (aiAnalysisId != null && aiAnalysisId > 0) {
            aiAnalysis = aiAnalysisRepository.findById(aiAnalysisId)
                    .orElseThrow(() -> {
                        log.error("[ChatService] 채팅방 생성 - 에러: 존재하지 않는 분석입니다. id={}", aiAnalysisId);
                        return new CustomException(ChatErrorCode.CHAT_ANALYSIS_NOT_FOUND);
                    });
        }

        AiChatRoom aiChatRoom = AiChatRoom.builder()
                .user(user)
                .title(createChatRoomRequest.getTitle())
                .aiAnalysis(aiAnalysis)
                .build();

        AiChatRoom savedChatRoom = aiChatRoomRepository.save(aiChatRoom);
        saveWelcomeMessage(savedChatRoom, GENERAL_WELCOME_MESSAGE);

        CreateChatRoomResponse response = CreateChatRoomResponse.from(savedChatRoom);

        log.info("[ChatService] 채팅방 생성 - 완료: 채팅방 제목={}", createChatRoomRequest.getTitle());

        return response;
    }

    // 채팅방 생성 시 안내 메시지를 AI 메시지로 저장한다.
    private void saveWelcomeMessage(AiChatRoom aiChatRoom, String content) {
        AiChatMessage welcomeMessage = AiChatMessage.builder()
                .chatRoom(aiChatRoom)
                .role(MessageRole.AI)
                .messageType(MessageType.TEXT)
                .content(content)
                .build();
        aiChatMessageRepository.save(welcomeMessage);
    }

    @Override
    public void deleteChatRoom(Long userId, Long chatRoomId) {

        log.info("[ChatService] 채팅방 삭제 - 시작: chatRoomId={}", chatRoomId);

        AiChatRoom aiChatRoom = aiChatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> {
                    log.error("[ChatService] 채팅방 삭제 - 에러: 해당 채팅방 식별자를 찾을 수 없습니다. chatRoomId={}", chatRoomId);
                    return new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
                });

        if (!aiChatRoom.getUser().getUserId().equals(userId)) {
            log.error("[ChatService] 채팅방 삭제 - 에러: 본인 소유의 채팅방이 아닙니다. userId={}, chatRoomId={}", userId, chatRoomId);
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        aiChatMessageRepository.deleteAllByChatRoom_ChatRoomId(chatRoomId);
        aiChatRoomRepository.delete(aiChatRoom);

        log.info("[ChatService] 채팅방 삭제 - 완료");
    }

    @Override
    public void sendMessage(Long userId, Long chatRoomId, String content) {

        log.info("[ChatService] 메시지 전송 - 시작: chatRoomId={}", chatRoomId);

        AiChatRoom aiChatRoom = aiChatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> {
                    log.error("[ChatService] 메시지 전송 - 에러: 해당 채팅방 식별자를 찾을 수 없습니다. chatRoomId={}", chatRoomId);
                    return new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
                });

        if (!aiChatRoom.getUser().getUserId().equals(userId)) {
            log.error("[ChatService] 메시지 전송 - 에러: 본인 소유의 채팅방이 아닙니다. userId={}, chatRoomId={}", userId, chatRoomId);
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        AiChatMessage userMessage = AiChatMessage.builder()
                .chatRoom(aiChatRoom)
                .role(MessageRole.USER)
                .messageType(MessageType.TEXT)
                .content(content)
                .build();
        aiChatMessageRepository.save(userMessage);
        broadcastAfterCommit(chatRoomId, ChatMessageResponse.from(userMessage));

        generateAndBroadcastAiReply(aiChatRoom);

        log.info("[ChatService] 메시지 전송 - 완료: chatRoomId={}", chatRoomId);
    }

    // 최근 대화 이력을 바탕으로 AI 응답을 생성해 저장하고 브로드캐스트한다.
    private void generateAndBroadcastAiReply(AiChatRoom aiChatRoom) {

        Long chatRoomId = aiChatRoom.getChatRoomId();

        List<AiChatMessage> history = aiChatMessageRepository
                .findByChatRoom_ChatRoomIdOrderByCreatedAtDesc(chatRoomId, PageRequest.of(0, OpenAiChatClient.MAX_HISTORY_SIZE))
                .getContent()
                .reversed();

        String analysisContext = buildAnalysisContext(aiChatRoom);

        String aiReply;
        try {
            aiReply = openAiChatClient.getReply(history, analysisContext);
        } catch (CustomException e) {
            log.error("[ChatService] AI 응답 생성 - 실패: chatRoomId={}, message={}", chatRoomId, e.getMessage());
            broadcastAfterCommit(chatRoomId, ChatErrorResponse.of(e.getMessage()));
            return;
        }

        AiChatMessage aiMessage = AiChatMessage.builder()
                .chatRoom(aiChatRoom)
                .role(MessageRole.AI)
                .messageType(MessageType.TEXT)
                .content(aiReply)
                .build();
        aiChatMessageRepository.save(aiMessage);
        broadcastAfterCommit(chatRoomId, ChatMessageResponse.from(aiMessage));
    }

    @Override
    public ChatMessageResponse uploadFile(Long userId, Long chatRoomId, MultipartFile file, String content) {

        log.info("[ChatService] 파일 업로드 - 시작: chatRoomId={}", chatRoomId);

        AiChatRoom aiChatRoom = aiChatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> {
                    log.error("[ChatService] 파일 업로드 - 에러: 해당 채팅방 식별자를 찾을 수 없습니다. chatRoomId={}", chatRoomId);
                    return new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
                });

        if (!aiChatRoom.getUser().getUserId().equals(userId)) {
            log.error("[ChatService] 파일 업로드 - 에러: 본인 소유의 채팅방이 아닙니다. userId={}, chatRoomId={}", userId, chatRoomId);
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        if (file == null || file.isEmpty()) {
            log.error("[ChatService] 파일 업로드 - 에러: 파일이 비어있습니다. chatRoomId={}", chatRoomId);
            throw new CustomException(ChatErrorCode.CHAT_FILE_EMPTY);
        }

        String fileUrl = imageStorageService.store(file, "chat");
        MessageType messageType = isImage(file) ? MessageType.IMAGE : MessageType.FILE;
        String messageContent = (content != null && !content.isBlank()) ? content : file.getOriginalFilename();

        AiChatMessage fileMessage = AiChatMessage.builder()
                .chatRoom(aiChatRoom)
                .role(MessageRole.USER)
                .messageType(messageType)
                .content(messageContent)
                .fileUrl(fileUrl)
                .build();
        aiChatMessageRepository.save(fileMessage);

        ChatMessageResponse response = ChatMessageResponse.from(fileMessage);
        broadcastAfterCommit(chatRoomId, response);

        generateAndBroadcastAiReply(aiChatRoom);

        log.info("[ChatService] 파일 업로드 - 완료: chatRoomId={}", chatRoomId);

        return response;
    }

    private boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    // 트랜잭션 커밋 이후에만 브로드캐스트 (롤백 시 존재하지 않는 메시지가 클라이언트에 노출되는 것 방지)
    private void broadcastAfterCommit(Long chatRoomId, Object payload) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend(CHAT_ROOM_TOPIC_PREFIX + chatRoomId, payload);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ChatMessagePageResponse getChatMessages(Long userId, Long chatRoomId, int page, int size) {

        log.info("[ChatService] 대화 내역 조회 - 시작: chatRoomId={}", chatRoomId);

        AiChatRoom aiChatRoom = aiChatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> {
                    log.error("[ChatService] 대화 내역 조회 - 에러: 해당 채팅방 식별자를 찾을 수 없습니다. chatRoomId={}", chatRoomId);
                    return new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
                });

        if (!aiChatRoom.getUser().getUserId().equals(userId)) {
            log.error("[ChatService] 대화 내역 조회 - 에러: 본인 소유의 채팅방이 아닙니다. userId={}, chatRoomId={}", userId, chatRoomId);
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        Slice<AiChatMessage> messages = aiChatMessageRepository
                .findByChatRoom_ChatRoomIdOrderByCreatedAtDesc(chatRoomId, PageRequest.of(page, size));

        log.info("[ChatService] 대화 내역 조회 - 완료: chatRoomId={}, 조회 결과={}건, hasNext={}",
                chatRoomId, messages.getContent().size(), messages.hasNext());

        return ChatMessagePageResponse.from(messages);
    }

    @Override
    public RenameChatRoomResponse renameChatRoom(Long userId, Long chatRoomId, String newName) {

        log.info("[ChatService] 채팅방 이름 변경 - 시작: chatRoomId={}", chatRoomId);

        AiChatRoom aiChatRoom = aiChatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> {
                    log.error("[ChatService] 채팅방 이름 변경 - 에러: 해당 채팅방 식별자를 찾을 수 없습니다. chatRoomId={}", chatRoomId);
                    return new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
                });

        if (!aiChatRoom.getUser().getUserId().equals(userId)) {
            log.error("[ChatService] 채팅방 이름 변경 - 에러: 본인 소유의 채팅방이 아닙니다. userId={}, chatRoomId={}", userId, chatRoomId);
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        aiChatRoomRepository.updateTitle(chatRoomId, newName);

        log.info("[ChatService] 채팅방 이름 변경 - 완료: chatRoomId={}", chatRoomId);

        return RenameChatRoomResponse.builder()
                .chatRoomId(chatRoomId)
                .title(newName)
                .build();
    }

    @Override
    public CreateChatRoomResponse findOrCreateChatRoomFromAnalysis(Long userId, Long analysisId) {

        log.info("[ChatService] 분석 기반 채팅방 조회/생성 - 시작: userId={}, analysisId={}", userId, analysisId);

        AiAnalysis aiAnalysis = aiAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> {
                    log.error("[ChatService] 분석 기반 채팅방 조회/생성 - 에러: 존재하지 않는 분석입니다. analysisId={}", analysisId);
                    return new CustomException(ChatErrorCode.CHAT_ANALYSIS_NOT_FOUND);
                });

        User user = aiAnalysis.getTodaySkin().getUser();
        if (!user.getUserId().equals(userId)) {
            log.error("[ChatService] 분석 기반 채팅방 조회/생성 - 에러: 본인 소유의 분석이 아닙니다. userId={}, analysisId={}", userId, analysisId);
            throw new CustomException(ChatErrorCode.CHAT_ANALYSIS_ACCESS_DENIED);
        }

        AiChatRoom aiChatRoom = aiChatRoomRepository.findByUser_UserIdAndAiAnalysis_AnalysisId(userId, analysisId)
                .orElseGet(() -> {
                    String logDate = aiAnalysis.getTodaySkin().getLogDate().format(TITLE_DATE_FORMATTER);
                    String title = logDate + " 피부 상담";
                    AiChatRoom newRoom = AiChatRoom.builder()
                            .user(user)
                            .title(title)
                            .aiAnalysis(aiAnalysis)
                            .build();
                    AiChatRoom savedNewRoom = aiChatRoomRepository.save(newRoom);

                    String analysisWelcomeMessage = "오늘의 분석 결과에서 어떤 부분이 궁금하신가요?\n→ " + logDate + " 투데이스킨 첨부됨";
                    saveWelcomeMessage(savedNewRoom, analysisWelcomeMessage);

                    return savedNewRoom;
                });

        log.info("[ChatService] 분석 기반 채팅방 조회/생성 - 완료: chatRoomId={}", aiChatRoom.getChatRoomId());

        return CreateChatRoomResponse.from(aiChatRoom);
    }

    // 채팅방이 분석 기록과 연결되어 있으면 그날의 지표를 AI 시스템 프롬프트에 덧붙일 컨텍스트로 만든다.
    private String buildAnalysisContext(AiChatRoom aiChatRoom) {
        AiAnalysis aiAnalysis = aiChatRoom.getAiAnalysis();
        if (aiAnalysis == null) {
            return null;
        }

        StringBuilder context = new StringBuilder()
                .append("사용자의 ").append(aiAnalysis.getTodaySkin().getLogDate()).append(" 피부 분석 기록: ")
                .append("종합점수 ").append(aiAnalysis.getOverallScore())
                .append("(").append(aiAnalysis.getSkinStatusLabel().toLabel()).append(")");

        detailedSkinAnalysisRepository.findByAiAnalysis(aiAnalysis).ifPresent(detail ->
                context.append(", 트러블 ").append(detail.getTrouble())
                        .append(", 유분 ").append(detail.getSebum())
                        .append(", 칙칙함 ").append(detail.getDullness())
                        .append(", 수분 ").append(detail.getMoisture())
                        .append(", 탄력 ").append(detail.getElasticity())
        );

        if (aiAnalysis.getAiComment() != null) {
            context.append(". 코멘트: ").append(aiAnalysis.getAiComment());
        }
        context.append(". 이 기록을 참고해서 사용자의 질문에 답변하세요.");

        return context.toString();
    }

}