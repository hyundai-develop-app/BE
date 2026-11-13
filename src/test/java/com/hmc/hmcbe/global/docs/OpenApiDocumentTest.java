package com.hmc.hmcbe.global.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmc.hmcbe.global.config.PostgisContainerConfig;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 실제 애플리케이션 컨텍스트로 OpenAPI 문서를 만들어 검증하고 {@code build/openapi/openapi.json}에 저장한다.
 * 저장한 파일은 {@code scripts/export_swagger_html.py}가 서버 없이 여는 Swagger HTML로 바꾼다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgisContainerConfig.class)
@ActiveProfiles("local")
@TestPropertySource(properties = {"spring.security.oauth2.resourceserver.jwt.issuer-uri=",
	"app.demo-auth.enabled=true"})
class OpenApiDocumentTest {

	private static final Path EXPORT_PATH = Path.of("build", "openapi", "openapi.json");

	@Autowired
	MockMvc mvc;
	@Autowired
	ObjectMapper mapper;

	private JsonNode document;

	@BeforeEach
	void loadDocument() throws Exception {
		String body = mvc.perform(get("/api-docs"))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		document = mapper.readTree(body);
	}

	@Test
	@DisplayName("모든 API는 500 예시를, 공개 API가 아니면 401 예시를 가진다")
	void everyOperationDocumentsCommonErrors() {
		for (Map.Entry<String, JsonNode> operation : operations()) {
			JsonNode responses = operation.getValue().path("responses");
			assertThat(exampleCodes(responses, "500")).as(operation.getKey()).containsExactly("COMMON5001");
			if (operation.getKey().equals("POST /api/v1/demo/token")) {
				assertThat(responses.has("401")).as(operation.getKey()).isFalse();
				assertThat(operation.getValue().path("security").isArray()).isTrue();
				assertThat(operation.getValue().path("security")).isEmpty();
			} else {
				assertThat(exampleCodes(responses, "401")).as(operation.getKey()).containsExactly("COMMON4011");
			}
		}
	}

	@Test
	@DisplayName("@ApiErrorCodes에 적은 도메인 에러 코드가 상태별 예시로 들어간다")
	void declaredErrorCodesBecomeExamples() {
		JsonNode waypoint = operation("put", "/api/v1/arrivals/{sessionId}/waypoint").path("responses");

		assertThat(exampleCodes(waypoint, "400")).containsExactly("COMMON4001", "ARRIVAL4002");
		assertThat(exampleCodes(waypoint, "404")).containsExactly("ARRIVAL4041", "PARKINGLOT4041");
		assertThat(exampleCodes(waypoint, "409")).containsExactly("ARRIVAL4091");
		JsonNode example = waypoint.at("/404/content/application~1json/examples/ARRIVAL4041/value");
		assertThat(example.path("success").asBoolean(true)).isFalse();
		assertThat(example.path("data").isNull()).isTrue();
		assertThat(example.at("/error/code").asText()).isEqualTo("ARRIVAL4041");
		assertThat(example.at("/error/message").asText()).isEqualTo("도착 모드를 찾을 수 없습니다");
	}

	@Test
	@DisplayName("생성 API는 201을 문서화하고 200을 두지 않는다")
	void createEndpointsDocument201() {
		for (String path : List.of("/api/v1/arrivals", "/api/v1/parking-records", "/api/v1/saved-places")) {
			JsonNode responses = operation("post", path).path("responses");
			assertThat(responses.has("201")).as(path).isTrue();
			assertThat(responses.has("200")).as(path).isFalse();
			assertThat(responses.at("/201/content/application~1json/schema/$ref").asText())
				.as(path).startsWith("#/components/schemas/ApiResponse");
		}
	}

	@Test
	@DisplayName("성공 응답 예시는 success=true, error=null이고 data를 DTO 예시 값으로 채운다")
	void successResponsesHaveWrappedExamples() {
		for (Map.Entry<String, JsonNode> operation : operations()) {
			operation.getValue().path("responses").fields().forEachRemaining(response -> {
				if (!response.getKey().startsWith("2")) {
					return;
				}
				JsonNode example = response.getValue().at("/content/application~1json/example");
				String label = operation.getKey() + " " + response.getKey();
				assertThat(example.path("success").asBoolean(false)).as(label).isTrue();
				assertThat(example.has("error") && example.path("error").isNull()).as(label).isTrue();
			});
		}

		JsonNode summary = operation("get", "/api/v1/parking-records/active/summary")
			.at("/responses/200/content/application~1json/example/data");
		assertThat(summary.at("/record/spotMemo").asText()).isEqualTo("지하 2층 C구역");
		assertThat(summary.at("/record/exitedAt").isNull()).isTrue();
		assertThat(summary.at("/quickDestinations/0/name").asText()).isEqualTo("집");
		JsonNode exit = operation("post", "/api/v1/parking-records/{recordId}/exit")
			.at("/responses/200/content/application~1json/example");
		assertThat(exit.path("data").isNull()).isTrue();
	}

	@Test
	@DisplayName("요청·응답 DTO 필드에 설명이 있다")
	void schemaFieldsAreDescribed() {
		JsonNode schemas = document.at("/components/schemas");
		List<String> undocumented = new ArrayList<>();
		schemas.fields().forEachRemaining(schema -> schema.getValue().path("properties").fields()
			.forEachRemaining(property -> {
				if (!property.getValue().hasNonNull("description")) {
					undocumented.add(schema.getKey() + "." + property.getKey());
				}
			}));

		assertThat(undocumented).isEmpty();
	}

	@Test
	@DisplayName("주차장 새 필드를 문서화하면 주소·요금 문구·주말 전용 설명과 예시를 제공한다")
	void parkingLotFrontFieldsAreDocumented() {
		// given
		JsonNode properties = document.at("/components/schemas/ParkingLotResponse/properties");

		// when & then
		assertThat(properties.path("address").path("description").asText()).contains("주소", "null");
		assertThat(properties.path("address").path("example").asText()).isEqualTo("서울 강남구 영동대로 513 지하");
		assertThat(properties.path("feeDescription").path("description").asText()).contains("요금", "null");
		assertThat(properties.path("feeDescription").path("example").asText()).isEqualTo("10분 800원");
		assertThat(properties.path("weekendOnly").path("description").asText()).contains("토·일", "공휴일", "평일");
		assertThat(properties.path("weekendOnly").path("example").asText()).isEqualTo("false");
	}

	@Test
	@DisplayName("OpenAPI 문서를 build/openapi/openapi.json에 저장한다")
	void exportDocument() throws Exception {
		Files.createDirectories(EXPORT_PATH.getParent());
		mapper.writerWithDefaultPrettyPrinter().writeValue(EXPORT_PATH.toFile(), document);

		assertThat(EXPORT_PATH).exists();
		assertThat(document.at("/info/title").asText()).isEqualTo("HMC-BE API");
	}

	@Test
	@DisplayName("경유지 해제 API를 문서화하면 200 세션 응답과 404·409 오류 예시를 제공한다")
	void clearWaypointIsDocumented() {
		// given
		JsonNode clear = operation("delete", "/api/v1/arrivals/{sessionId}/waypoint");

		// when
		JsonNode responses = clear.path("responses");
		List<JsonNode> sessionParameters = new ArrayList<>();
		clear.path("parameters").forEach(parameter -> {
			if (parameter.path("name").asText().equals("sessionId")) {
				sessionParameters.add(parameter);
			}
		});

		// then
		assertThat(clear.path("operationId").asText()).isEqualTo("clearWaypoint");
		assertThat(clear.path("summary").asText()).isEqualTo("경유 주차장 해제");
		assertThat(clear.path("description").asText()).contains("되돌리기", "원래 목적지", "경유지가 없어도 성공");
		assertThat(responses.at("/200/content/application~1json/schema/$ref").asText())
				.isEqualTo(operation("get", "/api/v1/arrivals/active")
						.at("/responses/200/content/application~1json/schema/$ref").asText())
				.startsWith("#/components/schemas/ApiResponse");
		assertThat(exampleCodes(responses, "404")).containsExactly("ARRIVAL4041");
		assertThat(exampleCodes(responses, "409")).containsExactly("ARRIVAL4091");
		assertThat(sessionParameters).hasSize(1);
		assertThat(sessionParameters.get(0).path("in").asText()).isEqualTo("path");
		assertThat(sessionParameters.get(0).path("description").asText()).isEqualTo("도착 모드 세션 ID");
		assertThat(sessionParameters.get(0).path("example").asText()).isEqualTo("10");
	}

	@Test
	@DisplayName("도착 모드 문서를 조회하면 700m 검색·목적지 추정·예측 없는 추천 규칙을 설명한다")
	void arrivalFrontParityRulesAreDocumented() {
		// given
		JsonNode schemas = document.at("/components/schemas");

		// when
		String briefing = operation("get", "/api/v1/arrivals/{sessionId}/briefing").path("description").asText();
		String count = schemas.at("/ArrivalOutlookResponse/properties/betterAlternativeCount/description").asText();
		String destination = schemas.at("/StartArrivalRequest/properties/destinationParkingLotId/description").asText();
		String alternatives = schemas.at("/ParkingBriefingResponse/properties/alternatives/description").asText();

		// then
		assertThat(briefing).contains("700m").doesNotContain("500m");
		assertThat(count).contains("700m").doesNotContain("500m");
		assertThat(destination).contains("700m", "가장 가까운");
		assertThat(alternatives).contains("예측", "큰", "가까운", "추천");
	}

	private List<Map.Entry<String, JsonNode>> operations() {
		List<Map.Entry<String, JsonNode>> operations = new ArrayList<>();
		document.path("paths").fields().forEachRemaining(path -> path.getValue().fields().forEachRemaining(
			method -> operations.add(Map.entry(method.getKey().toUpperCase() + " " + path.getKey(), method.getValue()))));
		assertThat(operations).isNotEmpty();
		return operations;
	}

	private JsonNode operation(String method, String path) {
		JsonNode operation = document.path("paths").path(path).path(method);
		assertThat(operation.isMissingNode()).as(method + " " + path).isFalse();
		return operation;
	}

	private List<String> exampleCodes(JsonNode responses, String status) {
		List<String> codes = new ArrayList<>();
		responses.path(status).path("content").path("application/json").path("examples").fieldNames()
			.forEachRemaining(codes::add);
		return codes;
	}
}
