package com.luna.skin.infra.openai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiContentPart(String type, String text, @JsonProperty("image_url") ImageUrl imageUrl) {

    public record ImageUrl(String url) {
    }

    public static OpenAiContentPart text(String text) {
        return new OpenAiContentPart("text", text, null);
    }

    public static OpenAiContentPart imageUrl(String url) {
        return new OpenAiContentPart("image_url", null, new ImageUrl(url));
    }
}
