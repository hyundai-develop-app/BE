package com.hmc.hmcbe.user.fixture;

import com.hmc.hmcbe.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

public final class UserFixture {

	public static final String AUTH_SUBJECT = "issuer-sub-1";

	private UserFixture() {
	}

	public static User user() {
		return User.create(AUTH_SUBJECT);
	}

	public static User userWithId(Long id) {
		User user = user();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
