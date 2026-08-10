package com.luna.skin.infra.openai.dto;

import aQute.bnd.annotation.jpms.Open;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenAiRoutineRequest {
        private String model;
        private List<OpenAiMessage> messages;
        private Map<String, String> response_format;

        // JSON 응답 직렬화
        public static OpenAiRoutineRequest from(String model, List<OpenAiMessage> messages) {
        return OpenAiRoutineRequest.builder()
                .model(model)
                .messages(messages)
                .response_format(Map.of("type", "json_object"))
                .build();
    }
}