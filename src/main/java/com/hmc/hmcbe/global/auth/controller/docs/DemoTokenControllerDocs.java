package com.hmc.hmcbe.global.auth.controller.docs;

import com.hmc.hmcbe.global.auth.dto.request.DemoTokenRequest;
import com.hmc.hmcbe.global.auth.dto.response.DemoTokenResponse;
import com.hmc.hmcbe.global.docs.PublicApi;
import com.hmc.hmcbe.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "DemoAuth", description = "데모 환경 전용 JWT 발급. 서버에서 DEMO_AUTH_ENABLED=true일 때만 열린다.")
public interface DemoTokenControllerDocs {

	@Operation(summary = "데모 JWT 발급 (명시적 활성화 필요)",
		description = "지정한 subject로 1시간 유효한 Bearer 토큰을 발급합니다. 서버 재시작 시 기존 토큰은 무효화됩니다. 인증 없이 호출합니다.")
	@PublicApi
	ResponseEntity<ApiResponse<DemoTokenResponse>> issue(DemoTokenRequest request);
}
