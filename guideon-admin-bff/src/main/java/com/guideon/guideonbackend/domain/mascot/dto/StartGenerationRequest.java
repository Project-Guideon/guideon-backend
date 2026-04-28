package com.guideon.guideonbackend.domain.mascot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "마스코트 3D 모델 생성 요청")
public class StartGenerationRequest {

    @Schema(description = "선업로드된 마스코트 이미지 URL (POST /mascot/image 응답값)",
            example = "https://realguideon.duckdns.org/internal/files/1/abc123.png")
    @NotBlank(message = "image_url은 필수입니다")
    @Size(max = 500, message = "image_url은 500자 이하여야 합니다")
    @JsonProperty("image_url")
    private String imageUrl;
}
