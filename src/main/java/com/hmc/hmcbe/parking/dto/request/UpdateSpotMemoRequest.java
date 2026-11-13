package com.hmc.hmcbe.parking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 음성 인식된 층·구역 문구. 예: "지하 2층 C구역". */
@Schema(description = "층·구역 저장 요청")
public record UpdateSpotMemoRequest(
		@Schema(description = "음성 인식된 층·구역 문구(최대 100자)", example = "지하 2층 C구역", maxLength = 100,
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank @Size(max = 100) String spotMemo) {
}
