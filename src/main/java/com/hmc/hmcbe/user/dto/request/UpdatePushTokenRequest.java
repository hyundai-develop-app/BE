package com.hmc.hmcbe.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "폰 푸시 토큰 등록 요청")
public record UpdatePushTokenRequest(
		@Schema(description = "폰 앱의 FCM 등록 토큰(최대 512자)", example = "fcm-token-abc123", maxLength = 512,
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank @Size(max = 512) String pushToken) {
}
