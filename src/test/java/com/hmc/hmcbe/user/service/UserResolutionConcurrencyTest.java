package com.hmc.hmcbe.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

import com.hmc.hmcbe.global.config.PostgisContainerConfig;
import com.hmc.hmcbe.user.entity.User;
import com.hmc.hmcbe.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@Import(PostgisContainerConfig.class)
class UserResolutionConcurrencyTest {

	@Autowired
	private UserWriteService userWriteService;

	@MockitoSpyBean
	private UserRepository userRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("첫 로그인 두 요청이 동시에 사용자 부재를 조회해도 같은 ID로 성공한다")
	void resolveSameNewSubjectConcurrently() throws Exception {
		String subject = "concurrent-" + UUID.randomUUID();
		CyclicBarrier bothReadMissingUser = new CyclicBarrier(2);
		AtomicInteger reads = new AtomicInteger();
		doAnswer(invocation -> {
			Optional<User> result = entityManager.createQuery(
					"select u from User u where u.authSubject = :subject", User.class)
					.setParameter("subject", subject)
					.getResultList().stream().findFirst();
			if (reads.incrementAndGet() <= 2) {
				assertThat(result).isEmpty();
				bothReadMissingUser.await(10, TimeUnit.SECONDS);
			}
			return result;
		}).when(userRepository).findByAuthSubject(subject);
		ExecutorService workers = Executors.newFixedThreadPool(2);
		try {
			Future<Long> first = workers.submit(() -> userWriteService.resolveUserId(subject));
			Future<Long> second = workers.submit(() -> userWriteService.resolveUserId(subject));

			Long firstId = first.get(20, TimeUnit.SECONDS);
			Long secondId = second.get(20, TimeUnit.SECONDS);

			assertThat(firstId).isNotNull().isEqualTo(secondId);
			assertThat(userRepository.findAll().stream().filter(user -> user.getAuthSubject().equals(subject)))
					.hasSize(1);
		} finally {
			workers.shutdownNow();
			workers.awaitTermination(5, TimeUnit.SECONDS);
			userRepository.findByAuthSubject(subject).ifPresent(userRepository::delete);
		}
	}
}
