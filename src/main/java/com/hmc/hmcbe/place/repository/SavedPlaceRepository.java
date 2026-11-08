package com.hmc.hmcbe.place.repository;

import com.hmc.hmcbe.place.entity.SavedPlace;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedPlaceRepository extends JpaRepository<SavedPlace, Long> {

	List<SavedPlace> findByUserIdOrderByIdAsc(Long userId);

	boolean existsByUserIdAndName(Long userId, String name);
}
