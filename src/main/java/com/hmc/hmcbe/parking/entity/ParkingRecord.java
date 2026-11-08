package com.hmc.hmcbe.parking.entity;

import static com.hmc.hmcbe.global.exception.DomainPreconditions.require;
import static com.hmc.hmcbe.global.exception.DomainPreconditions.requireNonNull;
import static com.hmc.hmcbe.global.exception.DomainPreconditions.requireText;

import com.hmc.hmcbe.global.entity.BaseTimeEntity;
import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.exception.ErrorCode;
import com.hmc.hmcbe.global.geo.GeoPoint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

/**
 * 기어 P 변속 시 자동 저장되는 주차 위치. 층·구역은 음성으로 덧붙이며, 대답이 없으면 GPS 위치만 남는다.
 * 사용자당 출차하지 않은 기록은 하나다(DB 부분 유니크 인덱스).
 */
@Getter
@Entity
@Table(name = "parking_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParkingRecord extends BaseTimeEntity {

	private static final int MAX_SPOT_MEMO_LENGTH = 100;

	@Column(nullable = false, updatable = false)
	private Long userId;

	@Column(updatable = false)
	private Long parkingLotId;

	@Column(nullable = false, updatable = false, columnDefinition = "geometry(Point, 4326)")
	private Point location;

	@Column(nullable = false, updatable = false)
	private Instant parkedAt;

	@Column(length = MAX_SPOT_MEMO_LENGTH)
	private String spotMemo;

	/** 폰 전송 시각. 푸시 발송 연동 전이라 아직 기록하지 않는다(DECISIONS D10). */
	private Instant sentToPhoneAt;

	private Instant exitedAt;

	private ParkingRecord(Long userId, Long parkingLotId, Point location, Instant parkedAt) {
		this.userId = userId;
		this.parkingLotId = parkingLotId;
		this.location = location;
		this.parkedAt = parkedAt;
	}

	public static ParkingRecord record(Long userId, Long parkingLotId, GeoPoint location, Instant parkedAt) {
		requireNonNull(userId, "사용자 ID");
		requireNonNull(location, "주차 위치");
		requireNonNull(parkedAt, "주차 시각");
		return new ParkingRecord(userId, parkingLotId, location.toPoint(), parkedAt);
	}

	public void updateSpotMemo(String spotMemo) {
		requireActive();
		this.spotMemo = requireText(spotMemo, MAX_SPOT_MEMO_LENGTH, "층·구역");
	}

	public void exit(Instant exitedAt) {
		requireActive();
		requireNonNull(exitedAt, "출차 시각");
		require(!exitedAt.isBefore(parkedAt), "출차 시각은 주차 시각보다 이를 수 없습니다");
		this.exitedAt = exitedAt;
	}

	public boolean isActive() {
		return exitedAt == null;
	}

	public boolean isOwnedBy(Long userId) {
		return this.userId.equals(userId);
	}

	public Duration elapsed(Instant now) {
		Duration elapsed = Duration.between(parkedAt, now);
		return elapsed.isNegative() ? Duration.ZERO : elapsed;
	}

	public Duration remainingFreeTime(Duration freeTime, Instant now) {
		Duration remaining = freeTime.minus(elapsed(now));
		return remaining.isNegative() ? Duration.ZERO : remaining;
	}

	public GeoPoint getLocation() {
		return GeoPoint.from(location);
	}

	private void requireActive() {
		if (!isActive()) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 출차한 주차 기록입니다");
		}
	}
}
