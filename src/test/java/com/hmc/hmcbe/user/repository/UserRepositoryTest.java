package com.hmc.hmcbe.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hmc.hmcbe.global.config.PostgisContainerConfig;
import com.hmc.hmcbe.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgisContainerConfig.class)
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("인증 subject로 사용자를 찾는다")
	void findByAuthSubject() {
		// given
		User saved = userRepository.saveAndFlush(User.create("sub-a"));

		// when & then
		assertThat(userRepository.findByAuthSubject("sub-a")).get()
				.extracting(User::getId).isEqualTo(saved.getId());
		assertThat(userRepository.findByAuthSubject("sub-b")).isEmpty();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("동일 subject를 중복 삽입해도 기존 사용자와 푸시 토큰은 유지된다")
	void insertIfAbsentKeepsExistingUser() {
		User existing = User.create("existing-upsert");
		existing.updatePushToken("keep-token");
		userRepository.saveAndFlush(existing);

		int inserted = userRepository.insertIfAbsent("existing-upsert");

		assertThat(inserted).isZero();
		User found = userRepository.findByAuthSubject("existing-upsert").orElseThrow();
		assertThat(found.getId()).isEqualTo(existing.getId());
		assertThat(found.getPushToken()).isEqualTo("keep-token");
	}

	@Test
	@DisplayName("처음 보는 subject를 충돌 무시 삽입하면 ID와 감사 시각이 저장된다")
	void insertIfAbsentCreatesUser() {
		int inserted = userRepository.insertIfAbsent("new-upsert");

		User found = userRepository.findByAuthSubject("new-upsert").orElseThrow();
		assertThat(inserted).isOne();
		assertThat(found.getId()).isNotNull();
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("같은 인증 subject로 두 사용자를 저장할 수 없다")
	void uniqueAuthSubject() {
		userRepository.saveAndFlush(User.create("dup"));

		assertThrows(DataIntegrityViolationException.class,
				() -> userRepository.saveAndFlush(User.create("dup")));
	}
}
