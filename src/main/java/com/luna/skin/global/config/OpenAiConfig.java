package com.luna.skin.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OpenAiConfig {

  @Value("${luna-skin.ai.openai.api-key}")
  private String apiKey;

  @Bean
  public RestTemplate openAiRestTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.getInterceptors().add((request, body, execution) -> {
      request.getHeaders().add("Authorization", "Bearer " + apiKey);
      request.getHeaders().add("Content-Type", "application/json");
      return execution.execute(request, body);
    });
    return restTemplate;
  }
}