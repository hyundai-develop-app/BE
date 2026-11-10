package com.hmc.hmcbe.global.exception;

import com.hmc.hmcbe.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex, HttpServletRequest request) {
		ErrorCode code = ex.getErrorCode();
		log.warn("BusinessException [{} {}] [{}]: {}", request.getMethod(), request.getRequestURI(), code.getCode(), ex.getMessage());
		return ResponseEntity.status(code.getStatus())
				.body(ApiResponse.fail(code, ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(e -> e.getField() + ": " + e.getDefaultMessage())
				.orElse(CommonErrorCode.INVALID_REQUEST.getDefaultMessage());
		log.warn("검증 실패 [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);
		return ResponseEntity.status(CommonErrorCode.INVALID_REQUEST.getStatus())
				.body(ApiResponse.fail(CommonErrorCode.INVALID_REQUEST, message));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
		log.warn("제약조건 위반 [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
		return ResponseEntity.status(CommonErrorCode.INVALID_REQUEST.getStatus())
				.body(ApiResponse.fail(CommonErrorCode.INVALID_REQUEST, ex.getMessage()));
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class})
	public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(Exception ex, HttpServletRequest request) {
		log.warn("잘못된 요청 형식 [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
		return ResponseEntity.status(CommonErrorCode.INVALID_REQUEST.getStatus())
				.body(ApiResponse.fail(CommonErrorCode.INVALID_REQUEST, CommonErrorCode.INVALID_REQUEST.getDefaultMessage()));
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException ex, HttpServletRequest request) {
		log.warn("인증 실패 [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
		return ResponseEntity.status(CommonErrorCode.UNAUTHORIZED.getStatus())
				.body(ApiResponse.fail(CommonErrorCode.UNAUTHORIZED, ex.getMessage()));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		log.warn("인가 실패 [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
		return ResponseEntity.status(CommonErrorCode.FORBIDDEN.getStatus())
				.body(ApiResponse.fail(CommonErrorCode.FORBIDDEN, ex.getMessage()));
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
		String message = "지원하지 않는 Content-Type입니다: " + request.getContentType();
		log.warn("미지원 미디어 타입 [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);
		return ResponseEntity.status(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.getStatus())
				.body(ApiResponse.fail(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE, message));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
		log.warn("없는 경로 [{} {}]", request.getMethod(), request.getRequestURI());
		return ResponseEntity.status(CommonErrorCode.NOT_FOUND.getStatus())
				.body(ApiResponse.fail(CommonErrorCode.NOT_FOUND, CommonErrorCode.NOT_FOUND.getDefaultMessage()));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
		String message = "지원하지 않는 HTTP 메서드입니다: " + ex.getMethod();
		log.warn("미지원 메서드 [{} {}]", request.getMethod(), request.getRequestURI());
		return ResponseEntity.status(CommonErrorCode.METHOD_NOT_ALLOWED.getStatus())
				.headers(ex.getHeaders())
				.body(ApiResponse.fail(CommonErrorCode.METHOD_NOT_ALLOWED, message));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("처리되지 않은 예외 [{} {}]", request.getMethod(), request.getRequestURI(), ex);
		return ResponseEntity.status(CommonErrorCode.INTERNAL_ERROR.getStatus())
				.body(ApiResponse.fail(CommonErrorCode.INTERNAL_ERROR, CommonErrorCode.INTERNAL_ERROR.getDefaultMessage()));
	}
}
