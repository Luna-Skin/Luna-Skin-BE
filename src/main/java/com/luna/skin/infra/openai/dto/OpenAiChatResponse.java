package com.luna.skin.infra.openai.dto;

import java.util.List;

public record OpenAiChatResponse(List<Choice> choices) {

    public record Choice(OpenAiMessage message) {
    }
}
