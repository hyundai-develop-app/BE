package com.hmc.hmcbe.global.docs;

import com.hmc.hmcbe.global.exception.CommonErrorCode;
import com.hmc.hmcbe.global.exception.ErrorCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.HandlerMethod;

/**
 * 각 엔드포인트의 Swagger 응답에 실패 응답 예시를 상태 코드별로 붙인다.
 *
 * <ul>
 * <li>{@link ApiErrorCodes}에 적은 도메인 에러 코드</li>
 * <li>요청 본문이나 파라미터가 있으면 400 {@code COMMON4001}(검증 실패, 형식 오류)</li>
 * <li>{@link PublicApi}가 아니면 401 {@code COMMON4011}</li>
 * <li>모든 엔드포인트에 500 {@code COMMON5001}</li>
 * </ul>
 * 에러 코드는 global이 도메인 패키지에 컴파일 의존하지 않도록 클래스패스에서 {@link ErrorCode} enum을 찾아 모은다.
 */
@Component
public class ErrorCodeOperationCustomizer implements OperationCustomizer {

	/** 실패 응답 본문 스키마 이름. OpenApiConfig가 components에 등록한다. */
	public static final String ERROR_SCHEMA = "ErrorResponse";
	private static final String ERROR_SCHEMA_REF = "#/components/schemas/" + ERROR_SCHEMA;
	private static final String APPLICATION_JSON = "application/json";
	private static final String BASE_PACKAGE = "com.hmc.hmcbe";

	private final Map<String, ErrorCode> errorCodes = scanErrorCodes();

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		boolean publicApi = handlerMethod.hasMethodAnnotation(PublicApi.class);
		if (publicApi) {
			operation.setSecurity(List.of());
		}
		Set<ErrorCode> codes = new LinkedHashSet<>();
		if (hasInput(operation)) {
			codes.add(CommonErrorCode.INVALID_REQUEST);
		}
		ApiErrorCodes declared = handlerMethod.getMethodAnnotation(ApiErrorCodes.class);
		if (declared != null) {
			Arrays.stream(declared.value()).map(code -> resolve(code, handlerMethod)).forEach(codes::add);
		}
		if (!publicApi) {
			codes.add(CommonErrorCode.UNAUTHORIZED);
		}
		codes.add(CommonErrorCode.INTERNAL_ERROR);
		addErrorResponses(operation, codes);
		return operation;
	}

	private boolean hasInput(Operation operation) {
		return operation.getRequestBody() != null
				|| (operation.getParameters() != null && !operation.getParameters().isEmpty());
	}

	private ErrorCode resolve(String code, HandlerMethod handlerMethod) {
		ErrorCode errorCode = errorCodes.get(code);
		if (errorCode == null) {
			throw new IllegalStateException("@ApiErrorCodes에 없는 에러 코드가 있습니다: " + code
					+ " (" + handlerMethod.getShortLogMessage() + ")");
		}
		return errorCode;
	}

	private void addErrorResponses(Operation operation, Set<ErrorCode> codes) {
		Map<Integer, List<ErrorCode>> byStatus = new TreeMap<>();
		for (ErrorCode code : codes) {
			byStatus.computeIfAbsent(code.getStatus().value(), status -> new ArrayList<>()).add(code);
		}
		ApiResponses responses = operation.getResponses() == null ? new ApiResponses() : operation.getResponses();
		byStatus.forEach((status, statusCodes) ->
				responses.addApiResponse(String.valueOf(status), errorResponse(status, statusCodes)));
		operation.setResponses(responses);
	}

	private ApiResponse errorResponse(int status, List<ErrorCode> codes) {
		MediaType mediaType = new MediaType().schema(new Schema<>().$ref(ERROR_SCHEMA_REF));
		for (ErrorCode code : codes) {
			mediaType.addExamples(code.getCode(), new Example()
					.summary(code.getCode() + " " + code.getDefaultMessage())
					.value(exampleBody(code)));
		}
		return new ApiResponse()
				.description(describe(status, codes))
				.content(new Content().addMediaType(APPLICATION_JSON, mediaType));
	}

	private String describe(int status, List<ErrorCode> codes) {
		StringBuilder description = new StringBuilder(HttpStatus.valueOf(status).getReasonPhrase());
		for (ErrorCode code : codes) {
			description.append("\n- `").append(code.getCode()).append("` ").append(code.getDefaultMessage());
			if (code == CommonErrorCode.INVALID_REQUEST) {
				description.append(" (필수 값 누락·형식 오류. 검증 실패면 message가 `필드명: 사유` 형태)");
			}
		}
		return description.toString();
	}

	private Map<String, Object> exampleBody(ErrorCode code) {
		Map<String, Object> error = new LinkedHashMap<>();
		error.put("code", code.getCode());
		error.put("message", code.getDefaultMessage());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("success", false);
		body.put("data", null);
		body.put("error", error);
		return body;
	}

	private static Map<String, ErrorCode> scanErrorCodes() {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AssignableTypeFilter(ErrorCode.class));
		Map<String, ErrorCode> codes = new LinkedHashMap<>();
		for (BeanDefinition candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
			Class<?> type = ClassUtils.resolveClassName(candidate.getBeanClassName(),
					ErrorCodeOperationCustomizer.class.getClassLoader());
			Object[] constants = type.getEnumConstants();
			if (constants == null) {
				continue;
			}
			for (Object constant : constants) {
				ErrorCode code = (ErrorCode) constant;
				codes.put(code.getCode(), code);
			}
		}
		return Map.copyOf(codes);
	}
}
