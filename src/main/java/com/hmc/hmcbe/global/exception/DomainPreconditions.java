package com.hmc.hmcbe.global.exception;

/** 엔티티·값 객체가 생성/변경 시 불변식을 검증할 때 쓰는 공통 검사. 위반하면 INVALID_REQUEST를 던진다. */
public final class DomainPreconditions {

	private DomainPreconditions() {
	}

	public static <T> T requireNonNull(T value, String field) {
		if (value == null) {
			throw invalid(field + "은(는) 필수입니다");
		}
		return value;
	}

	public static String requireText(String value, int maxLength, String field) {
		if (value == null || value.isBlank()) {
			throw invalid(field + "은(는) 비어 있을 수 없습니다");
		}
		return requireMaxLength(value, maxLength, field);
	}

	public static String requireMaxLength(String value, int maxLength, String field) {
		if (value != null && value.length() > maxLength) {
			throw invalid(field + "은(는) " + maxLength + "자를 넘을 수 없습니다");
		}
		return value;
	}

	public static void require(boolean condition, String message) {
		if (!condition) {
			throw invalid(message);
		}
	}

	private static BusinessException invalid(String message) {
		return new BusinessException(ErrorCode.INVALID_REQUEST, message);
	}
}
