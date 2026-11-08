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
	@DisplayName("같은 인증 subject로 두 사용자를 저장할 수 없다")
	void uniqueAuthSubject() {
		userRepository.saveAndFlush(User.create("dup"));

		assertThrows(DataIntegrityViolationException.class,
				() -> userRepository.saveAndFlush(User.create("dup")));
	}
}
