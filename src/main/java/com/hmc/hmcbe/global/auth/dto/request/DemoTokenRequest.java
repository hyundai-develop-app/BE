package com.hmc.hmcbe.global.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DemoTokenRequest(@NotBlank @Size(max = 255) String subject) {
}
