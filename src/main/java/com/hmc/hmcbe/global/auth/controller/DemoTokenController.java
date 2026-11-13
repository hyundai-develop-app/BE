package com.hmc.hmcbe.global.auth.controller;

import com.hmc.hmcbe.global.auth.DemoTokenIssuer;
import com.hmc.hmcbe.global.auth.controller.docs.DemoTokenControllerDocs;
import com.hmc.hmcbe.global.auth.dto.request.DemoTokenRequest;
import com.hmc.hmcbe.global.auth.dto.response.DemoTokenResponse;
import com.hmc.hmcbe.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local & !prod")
@RequiredArgsConstructor
public class DemoTokenController implements DemoTokenControllerDocs {

	private final DemoTokenIssuer demoTokenIssuer;

	@Override
	@PostMapping("/api/v1/demo/token")
	public ResponseEntity<ApiResponse<DemoTokenResponse>> issue(@Valid @RequestBody DemoTokenRequest request) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
			.body(ApiResponse.ok(demoTokenIssuer.issue(request.subject())));
	}
}
