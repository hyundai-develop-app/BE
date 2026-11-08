package com.hmc.hmcbe.user.service;

public interface UserWriteService {

	/** 인증 subject에 해당하는 사용자 ID를 반환하고, 처음 보는 subject면 사용자를 만든다. */
	Long resolveUserId(String authSubject);

	void updatePushToken(Long userId, String pushToken);
}
