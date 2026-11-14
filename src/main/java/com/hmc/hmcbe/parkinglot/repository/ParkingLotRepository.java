package com.hmc.hmcbe.parkinglot.repository;

import com.hmc.hmcbe.parkinglot.entity.ParkingLot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {

	/** 검색 반경·결과 수 제한과 무관하게 지정한 주차장의 기준 위치까지 거리를 조회한다. */
	@Query(value = """
			SELECT p.id AS id,
				ST_Distance(p.location::geography,
							ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) AS distanceMeters
			FROM parking_lots p
			WHERE p.id = :id
			""", nativeQuery = true)
	Optional<NearbyParkingLotProjection> findNearbyById(@Param("id") Long id,
			@Param("latitude") double latitude, @Param("longitude") double longitude);

	/** 미터 단위 반경 안의 주차장을 가까운 순으로 조회한다. 공간 쿼리라 네이티브 쿼리 + 프로젝션을 쓴다(3.3). */
	@Query(value = """
			SELECT p.id AS id,
				ST_Distance(p.location::geography,
							ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) AS distanceMeters
			FROM parking_lots p
			WHERE ST_DWithin(p.location::geography,
							ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
							:radiusMeters)
				AND (:includeWeekendOnly OR p.weekend_only = FALSE)
			ORDER BY distanceMeters
			LIMIT :limit
			""", nativeQuery = true)
	List<NearbyParkingLotProjection> findNearby(@Param("latitude") double latitude,
			@Param("longitude") double longitude,
			@Param("radiusMeters") double radiusMeters,
			@Param("limit") int limit,
			@Param("includeWeekendOnly") boolean includeWeekendOnly);
}
