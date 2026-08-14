package com.luna.skin.infra.openai.dto;

public record OpenAiRequestMessage(String role, Object content) {
}
