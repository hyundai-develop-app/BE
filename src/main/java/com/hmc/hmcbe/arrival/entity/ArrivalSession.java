package com.hmc.hmcbe.arrival.entity;

import static com.hmc.hmcbe.global.exception.DomainPreconditions.require;
import static com.hmc.hmcbe.global.exception.DomainPreconditions.requireNonNull;
import static com.hmc.hmcbe.global.exception.DomainPreconditions.requireText;

import com.hmc.hmcbe.arrival.exception.ArrivalErrorCode;
import com.hmc.hmcbe.global.entity.BaseTimeEntity;
import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.geo.GeoPoint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

/** 목적지를 설정하면 켜지는 "도착 모드". 사용자당 ACTIVE 세션은 하나다(DB 부분 유니크 인덱스). */
@Getter
@Entity
@Table(name = "arrival_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArrivalSession extends BaseTimeEntity {

	private static final int MAX_DESTINATION_NAME_LENGTH = 100;

	@Column(nullable = false, updatable = false)
	private Long userId;

	@Column(nullable = false, length = MAX_DESTINATION_NAME_LENGTH)
	private String destinationName;

	@Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
	private Point destinationLocation;

	private Long destinationParkingLotId;

	private Long waypointParkingLotId;

	@Column(nullable = false)
	private Instant expectedArrivalAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ArrivalStatus status;

	private Instant endedAt;

	private ArrivalSession(Long userId, String destinationName, Point destinationLocation,
			Long destinationParkingLotId, Instant expectedArrivalAt) {
		this.userId = userId;
		this.destinationName = destinationName;
		this.destinationLocation = destinationLocation;
		this.destinationParkingLotId = destinationParkingLotId;
		this.expectedArrivalAt = expectedArrivalAt;
		this.status = ArrivalStatus.ACTIVE;
	}

	public static ArrivalSession start(Long userId, String destinationName, GeoPoint destinationLocation,
			Long destinationParkingLotId, Instant expectedArrivalAt) {
		requireNonNull(userId, "사용자 ID", ArrivalErrorCode.INVALID_ARRIVAL_SESSION);
		requireText(destinationName, MAX_DESTINATION_NAME_LENGTH, "목적지 이름", ArrivalErrorCode.INVALID_ARRIVAL_SESSION);
		requireNonNull(destinationLocation, "목적지 위치", ArrivalErrorCode.INVALID_ARRIVAL_SESSION);
		requireNonNull(expectedArrivalAt, "예상 도착 시각", ArrivalErrorCode.INVALID_ARRIVAL_SESSION);
		return new ArrivalSession(userId, destinationName, destinationLocation.toPoint(),
				destinationParkingLotId, expectedArrivalAt);
	}

	public void selectWaypoint(Long parkingLotId) {
		requireActive();
		requireNonNull(parkingLotId, "경유 주차장 ID", ArrivalErrorCode.INVALID_WAYPOINT);
		require(!parkingLotId.equals(destinationParkingLotId), "목적지 주차장은 경유지로 넣을 수 없습니다", ArrivalErrorCode.INVALID_WAYPOINT);
		this.waypointParkingLotId = parkingLotId;
	}

	public void complete(Instant now) {
		end(ArrivalStatus.COMPLETED, now);
	}

	public void cancel(Instant now) {
		end(ArrivalStatus.CANCELLED, now);
	}

	public boolean isOwnedBy(Long userId) {
		return this.userId.equals(userId);
	}

	public GeoPoint getDestinationLocation() {
		return GeoPoint.from(destinationLocation);
	}

	private void end(ArrivalStatus endStatus, Instant now) {
		requireActive();
		this.status = endStatus;
		this.endedAt = requireNonNull(now, "종료 시각", ArrivalErrorCode.INVALID_ARRIVAL_SESSION);
	}

	private void requireActive() {
		if (status != ArrivalStatus.ACTIVE) {
			throw new BusinessException(ArrivalErrorCode.ALREADY_ENDED);
		}
	}
}
