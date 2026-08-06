package com.luna.skin.infra.openai.dto;

import java.util.List;

public record OpenAiChatRequest(String model, List<OpenAiMessage> messages) {
}
