package com.hmc.hmcbe.global.docs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * 성공 응답(2xx)에 {@code {"success": true, "data": ..., "error": null}} 형태의 예시를 붙인다.
 *
 * <p>Swagger UI가 스키마로 만드는 기본 예시는 {@code error}까지 채워서 성공 응답처럼 보이지 않는다.
 * data 예시는 DTO 필드의 {@code @Schema(example)}로 만들고, 예시가 없는 단순 값은 null로 둔다.
 */
@Component
public class SuccessExampleOpenApiCustomizer implements OpenApiCustomizer {

	private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";
	private static final String WRAPPER_PREFIX = "ApiResponse";
	private static final int MAX_DEPTH = 8;
	private static final String BASE_PACKAGE = "com.hmc.hmcbe";

	private final Map<String, Class<?>> dtoTypes = scanDtoTypes();

	@Override
	public void customise(OpenAPI openApi) {
		if (openApi.getPaths() == null || openApi.getComponents() == null) {
			return;
		}
		Map<String, Schema> schemas = openApi.getComponents().getSchemas();
		schemas.forEach(this::keepRefPropertyDescriptions);
		openApi.getPaths().values().forEach(path -> path.readOperations()
				.forEach(operation -> addSuccessExamples(operation, schemas)));
	}

	/**
	 * OpenAPI 3.0은 {@code $ref} 옆의 description·nullable을 버린다. DTO 필드의 {@code @Schema}를 읽어
	 * 참조 필드를 {@code allOf: [$ref]}로 감싸고 설명과 null 가능 여부를 되살린다.
	 */
	private void keepRefPropertyDescriptions(String schemaName, Schema<?> schema) {
		Class<?> type = schemaName.startsWith(WRAPPER_PREFIX) ? com.hmc.hmcbe.global.response.ApiResponse.class
				: dtoTypes.get(schemaName);
		if (type == null || schema.getProperties() == null) {
			return;
		}
		schema.getProperties().replaceAll((name, property) -> {
			io.swagger.v3.oas.annotations.media.Schema annotation = fieldSchema(type, name);
			if (property.get$ref() == null || annotation == null || annotation.description().isEmpty()) {
				return property;
			}
			Schema<Object> wrapped = new Schema<>();
			wrapped.addAllOfItem(new Schema<>().$ref(property.get$ref()));
			wrapped.setDescription(annotation.description());
			if (annotation.nullable()) {
				wrapped.setNullable(true);
			}
			return wrapped;
		});
	}

	private io.swagger.v3.oas.annotations.media.Schema fieldSchema(Class<?> type, String fieldName) {
		try {
			return type.getDeclaredField(fieldName).getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
		} catch (NoSuchFieldException e) {
			return null;
		}
	}

	/** springdoc은 DTO 스키마 이름으로 클래스 단순 이름을 쓴다. */
	private static Map<String, Class<?>> scanDtoTypes() {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(io.swagger.v3.oas.annotations.media.Schema.class));
		Map<String, Class<?>> types = new HashMap<>();
		for (BeanDefinition candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
			Class<?> type = ClassUtils.resolveClassName(candidate.getBeanClassName(),
					SuccessExampleOpenApiCustomizer.class.getClassLoader());
			types.put(type.getSimpleName(), type);
		}
		return Map.copyOf(types);
	}

	private void addSuccessExamples(Operation operation, Map<String, Schema> schemas) {
		if (operation.getResponses() == null) {
			return;
		}
		operation.getResponses().forEach((status, response) -> {
			if (status.startsWith("2")) {
				addSuccessExample(response, schemas);
			}
		});
	}

	private void addSuccessExample(ApiResponse response, Map<String, Schema> schemas) {
		if (response.getContent() == null) {
			return;
		}
		for (MediaType mediaType : response.getContent().values()) {
			Schema<?> schema = mediaType.getSchema();
			String name = schema == null ? null : refName(schema);
			if (name == null || !name.startsWith(WRAPPER_PREFIX) || !schemas.containsKey(name)) {
				continue;
			}
			Schema<?> data = (Schema<?>) schemas.get(name).getProperties().get("data");
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("success", true);
			body.put("data", data == null || isUntypedObject(data) ? null : example(data, schemas, 0));
			body.put("error", null);
			mediaType.setExample(body);
		}
	}

	private Object example(Schema<?> schema, Map<String, Schema> schemas, int depth) {
		if (depth > MAX_DEPTH) {
			return null;
		}
		String name = refName(schema);
		if (name != null) {
			Schema<?> resolved = schemas.get(name);
			return resolved == null ? null : example(resolved, schemas, depth + 1);
		}
		if (schema.getExample() != null) {
			return schema.getExample();
		}
		if (schema.getAllOf() != null && !schema.getAllOf().isEmpty()) {
			return example(schema.getAllOf().get(0), schemas, depth + 1);
		}
		if (schema.getItems() != null) {
			List<Object> items = new ArrayList<>();
			items.add(example(schema.getItems(), schemas, depth + 1));
			return items;
		}
		if (schema.getProperties() != null) {
			Map<String, Object> object = new LinkedHashMap<>();
			schema.getProperties().forEach((property, propertySchema) ->
					object.put(property, example(propertySchema, schemas, depth + 1)));
			return object;
		}
		return null;
	}

	private boolean isUntypedObject(Schema<?> schema) {
		return refName(schema) == null && schema.getProperties() == null && schema.getItems() == null
				&& schema.getAllOf() == null;
	}

	private String refName(Schema<?> schema) {
		String ref = schema.get$ref();
		return ref != null && ref.startsWith(SCHEMA_REF_PREFIX) ? ref.substring(SCHEMA_REF_PREFIX.length()) : null;
	}
}
