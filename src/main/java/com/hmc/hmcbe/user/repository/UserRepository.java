package com.hmc.hmcbe.user.repository;

import com.hmc.hmcbe.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByAuthSubject(String authSubject);

	@Modifying
	@Query(value = """
			INSERT INTO users (auth_subject, created_at, updated_at)
			VALUES (:authSubject, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
			ON CONFLICT (auth_subject) DO NOTHING
			""", nativeQuery = true)
	int insertIfAbsent(@Param("authSubject") String authSubject);
}
