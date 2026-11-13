package com.hmc.hmcbe.global.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "데모 토큰 발급 요청")
public record DemoTokenRequest(
		@Schema(description = "토큰 subject(최대 255자). 같은 값이면 같은 사용자로 취급한다.", example = "demo-driver",
				maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank @Size(max = 255) String subject) {
}
