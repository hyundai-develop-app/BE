package com.hmc.hmcbe.global.docs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 인증 없이 호출하는 엔드포인트임을 Swagger 문서에 표시한다. 문서에서 Bearer 요구와 401 응답을 뺀다.
 * 실제 접근 허용은 SecurityFilterChain이 결정하며, 이 애너테이션은 문서에만 영향을 준다.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicApi {
}
