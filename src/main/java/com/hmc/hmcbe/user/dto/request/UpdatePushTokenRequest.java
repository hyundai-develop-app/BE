package com.hmc.hmcbe.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePushTokenRequest(@NotBlank @Size(max = 512) String pushToken) {
}
