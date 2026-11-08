package com.hmc.hmcbe.user.entity;

import com.hmc.hmcbe.global.entity.BaseTimeEntity;
import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 외부 Issuer가 발급한 JWT의 subject를 내부 ID와 연결하는 사용자. 로그인/발급은 이 서비스 밖에서 처리한다. */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

	private static final int MAX_AUTH_SUBJECT_LENGTH = 255;
	private static final int MAX_PUSH_TOKEN_LENGTH = 512;

	@Column(nullable = false, unique = true, length = MAX_AUTH_SUBJECT_LENGTH)
	private String authSubject;

	@Column(length = MAX_PUSH_TOKEN_LENGTH)
	private String pushToken;

	private User(String authSubject) {
		this.authSubject = authSubject;
	}

	public static User create(String authSubject) {
		requireText(authSubject, MAX_AUTH_SUBJECT_LENGTH, "인증 subject");
		return new User(authSubject);
	}

	public void updatePushToken(String pushToken) {
		requireText(pushToken, MAX_PUSH_TOKEN_LENGTH, "푸시 토큰");
		this.pushToken = pushToken;
	}

	private static void requireText(String value, int maxLength, String field) {
		if (value == null || value.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, field + "은(는) 비어 있을 수 없습니다");
		}
		if (value.length() > maxLength) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, field + "은(는) " + maxLength + "자를 넘을 수 없습니다");
		}
	}
}
