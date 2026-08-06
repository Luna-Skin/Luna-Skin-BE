package com.luna.skin.infra.openai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiConfigTest {

    @Test
    void openAiRestTemplate은_Authorization과_ContentType_헤더를_중복없이_한번만_설정한다() throws Exception {
        CompletableFuture<Map<String, List<String>>> capturedHeaders = new CompletableFuture<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/probe", exchange -> {
            capturedHeaders.complete(Map.copyOf(exchange.getRequestHeaders()));
            exchange.getRequestBody().readAllBytes();
            byte[] resp = "{}".getBytes();
            exchange.sendResponseHeaders(200, resp.length);
            exchange.getResponseBody().write(resp);
            exchange.close();
        });
        server.start();

        try {
            OpenAiConfig config = new OpenAiConfig();
            ReflectionTestUtils.setField(config, "apiKey", "test-key");
            RestTemplate restTemplate = config.openAiRestTemplate();

            String url = "http://localhost:" + server.getAddress().getPort() + "/probe";
            restTemplate.postForObject(url, Map.of("hello", "world"), String.class);

            Map<String, List<String>> headers = capturedHeaders.get(5, TimeUnit.SECONDS);
            assertThat(headers.get("Content-type")).containsExactly("application/json");
            assertThat(headers.get("Authorization")).containsExactly("Bearer test-key");
        } finally {
            server.stop(0);
        }
    }
}
