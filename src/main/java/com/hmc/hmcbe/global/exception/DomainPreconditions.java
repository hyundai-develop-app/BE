package com.hmc.hmcbe.global.exception;

/**
 * 엔티티·값 객체가 생성/변경 시 불변식을 검증할 때 쓰는 공통 검사.
 * 위반하면 호출한 도메인이 넘긴 {@link ErrorCode}로 {@link BusinessException}을 던진다.
 */
public final class DomainPreconditions {

	private DomainPreconditions() {
	}

	public static <T> T requireNonNull(T value, String field, ErrorCode errorCode) {
		if (value == null) {
			throw new BusinessException(errorCode, field + "은(는) 필수입니다");
		}
		return value;
	}

	public static String requireText(String value, int maxLength, String field, ErrorCode errorCode) {
		if (value == null || value.isBlank()) {
			throw new BusinessException(errorCode, field + "은(는) 비어 있을 수 없습니다");
		}
		return requireMaxLength(value, maxLength, field, errorCode);
	}

	public static String requireMaxLength(String value, int maxLength, String field, ErrorCode errorCode) {
		if (value != null && value.length() > maxLength) {
			throw new BusinessException(errorCode, field + "은(는) " + maxLength + "자를 넘을 수 없습니다");
		}
		return value;
	}

	public static void require(boolean condition, String message, ErrorCode errorCode) {
		if (!condition) {
			throw new BusinessException(errorCode, message);
		}
	}
}
