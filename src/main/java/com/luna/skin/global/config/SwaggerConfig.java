package com.luna.skin.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LunaSkin API")
                        .description("""
                                LunaSkin 생리 주기 기반 스킨케어 트래킹 서비스 API 명세입니다.
                                
                                X-USER-ID 헤더에 유저 ID를 입력해 테스트하세요.
                                예: X-USER-ID: 1
                                """)
                        .version("v1"))
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description("Current server")
                ))
                .addSecurityItem(new SecurityRequirement().addList("X-USER-ID"))
                .components(new Components()
                        .addSecuritySchemes("X-USER-ID", new SecurityScheme()
                                .name("X-USER-ID")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)));
    }
}