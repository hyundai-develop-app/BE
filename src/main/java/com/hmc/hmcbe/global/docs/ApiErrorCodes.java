package com.hmc.hmcbe.global.docs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 엔드포인트가 응답할 수 있는 도메인 에러 코드를 Swagger 문서에 예시 응답으로 싣는다.
 *
 * <p>값은 {@link com.hmc.hmcbe.global.exception.ErrorCode#getCode()} 문자열(예: {@code ARRIVAL4041})이다.
 * 다른 도메인의 ErrorCode enum을 직접 참조하지 않도록 문자열로 받는다. 없는 코드를 쓰면 문서 생성이 실패한다.
 * 공통 에러(400 요청 형식, 401 인증, 500)는 {@link ErrorCodeOperationCustomizer}가 자동으로 붙이므로 적지 않는다.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodes {

	String[] value();
}
