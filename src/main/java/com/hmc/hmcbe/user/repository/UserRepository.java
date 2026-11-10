package com.hmc.hmcbe.user.repository;

import com.hmc.hmcbe.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByAuthSubject(String authSubject);
}
