package com.hmc.hmcbe.global.config;

import com.hmc.hmcbe.global.docs.ErrorCodeOperationCustomizer;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String DESCRIPTION = """
			HMC 주차 도우미 백엔드 API.

			## 공통 응답 형식
			모든 응답은 같은 껍데기(`ApiResponse`)를 쓴다.

			성공
			```json
			{ "success": true, "data": { ... }, "error": null }
			```
			본문이 없는 성공(삭제, 완료 등)은 `data`가 `null`이다.

			실패
			```json
			{ "success": false, "data": null, "error": { "code": "PARKING4041", "message": "주차 기록을 찾을 수 없습니다" } }
			```

			## 에러 코드
			- 형식: `{도메인}{HTTP 상태 3자리}{순번}`. 예: `PARKING4041` = parking 도메인, 404, 1번.
			- 분기는 `error.code`로 한다. `message`는 사람이 읽는 문구라서 상황에 따라 더 구체적일 수 있다.
			- 엔드포인트별로 나올 수 있는 코드는 각 API의 응답 목록에 예시로 있다. 상태 코드 하나에 여러 코드가 있으면 Examples 드롭다운에서 고른다.
			- 공통으로 붙는 코드: `COMMON4001`(400, 입력이 있는 API의 요청 형식·검증 실패), `COMMON4011`(401, 토큰 없음·만료·위조), `COMMON5001`(500).
			- 그 밖의 공통: `COMMON4041`(없는 경로), `COMMON4051`(지원하지 않는 메서드), `COMMON4151`(JSON이 아닌 Content-Type).

			## 인증
			`Authorization: Bearer {JWT}` 헤더를 보낸다. 사용자는 JWT `sub`로 식별하며, 처음 보는 `sub`면 사용자를 자동으로 만든다.
			데모 환경에서는 `POST /api/v1/demo/token`으로 토큰을 받는다(1시간 유효, 서버 재시작 시 무효).

			## 값 규칙
			- 시각은 ISO-8601 UTC 문자열이다. 예: `2026-11-14T09:30:00Z`. 요청에는 `+09:00` 같은 오프셋도 쓸 수 있다.
			- 좌표는 WGS84 위도(`latitude`, -90 ~ 90)와 경도(`longitude`, -180 ~ 180)다.
			- 다른 사용자의 리소스 ID로 요청하면 존재를 숨기기 위해 403이 아니라 404로 응답한다.
			""";

	@Bean
	OpenAPI openAPI() {
		return new OpenAPI()
			.info(new Info().title("HMC-BE API").version("v1").description(DESCRIPTION))
			.components(new Components()
				.addSecuritySchemes("bearerAuth", new SecurityScheme()
					.type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
				.addSchemas(ErrorCodeOperationCustomizer.ERROR_SCHEMA, errorResponseSchema()))
			.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
	}

	private Schema<?> errorResponseSchema() {
		Schema<?> errorBody = new ObjectSchema()
			.description("실패 정보")
			.addProperty("code", new StringSchema().description("에러 코드. 분기는 이 값으로 한다.").example("PARKING4041"))
			.addProperty("message", new StringSchema().description("사람이 읽는 에러 메시지").example("주차 기록을 찾을 수 없습니다"));
		errorBody.setRequired(List.of("code", "message"));
		Schema<?> data = new ObjectSchema().description("실패 시 항상 null").nullable(true);
		Schema<?> response = new ObjectSchema()
			.description("실패 응답")
			.addProperty("success", new BooleanSchema().description("실패 시 항상 false").example(false))
			.addProperty("data", data)
			.addProperty("error", errorBody);
		response.setRequired(List.of("success", "data", "error"));
		return response;
	}
}
