package com.guideon.guideonbackend.global.config;

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
        // JWT 보안 스키마 이름
        String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                // API 정보
                .info(new Info()
                        .title("Guideon Backend API")
                        .description("관광 키오스크 백엔드 API 문서")
                        .version("1.0.0")
                )
                // 서버 정보
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("로컬 개발 서버 (Admin BFF)")
                ))
                // JWT 보안 스키마 정의
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Access Token을 입력하세요 (Bearer 접두사 제외)")
                        )
                )
                // 전역 보안 요구사항 (모든 API에 적용)
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }
}
