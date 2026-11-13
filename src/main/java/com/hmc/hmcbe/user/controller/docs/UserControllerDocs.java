package com.hmc.hmcbe.user.controller.docs;

import com.hmc.hmcbe.global.docs.ApiErrorCodes;
import com.hmc.hmcbe.global.response.ApiResponse;
import com.hmc.hmcbe.user.dto.request.UpdatePushTokenRequest;
import com.hmc.hmcbe.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "User", description = "내 정보와 폰 푸시 토큰")
public interface UserControllerDocs {

	@Operation(summary = "내 정보 조회", description = "JWT subject에 해당하는 사용자. 처음 요청이면 사용자를 만든다.")
	@ApiErrorCodes({"USER4041"})
	ResponseEntity<ApiResponse<UserResponse>> getMe(Long userId);

	@Operation(summary = "폰 푸시 토큰 등록", description = "주차 위치를 폰으로 보낼 때 쓰는 토큰. 기존 토큰을 교체한다. 성공하면 data는 null.")
	@ApiErrorCodes({"USER4002", "USER4041"})
	ResponseEntity<ApiResponse<Void>> updatePushToken(Long userId, UpdatePushTokenRequest request);
}
