package com.hmc.hmcbe.parking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 음성 인식된 층·구역 문구. 예: "지하 2층 C구역". */
public record UpdateSpotMemoRequest(@NotBlank @Size(max = 100) String spotMemo) {
}
