package com.hmc.hmcbe.parkinglot.entity;

import static com.hmc.hmcbe.global.exception.DomainPreconditions.require;
import static com.hmc.hmcbe.global.exception.DomainPreconditions.requireNonNull;

import com.hmc.hmcbe.global.entity.BaseTimeEntity;
import com.hmc.hmcbe.parkinglot.exception.ParkingLotErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주차장의 특정 시각 예측 만차율. 주차장과 함께 바뀌어야 하는 불변식이 없어 별도 Aggregate로 두고 ID로 참조한다(7.2, 7.3).
 * 같은 도메인이라 DB에는 FK가 있다(DECISIONS D5).
 */
@Getter
@Entity
@Table(name = "parking_lot_occupancy_forecasts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OccupancyForecast extends BaseTimeEntity {

	@Column(nullable = false, updatable = false)
	private Long parkingLotId;

	@Column(nullable = false, updatable = false)
	private Instant targetAt;

	@Column(nullable = false)
	private short occupancyRate;

	private OccupancyForecast(Long parkingLotId, Instant targetAt, int occupancyRate) {
		this.parkingLotId = parkingLotId;
		this.targetAt = targetAt;
		this.occupancyRate = (short) occupancyRate;
	}

	public static OccupancyForecast create(Long parkingLotId, Instant targetAt, int occupancyRate) {
		requireNonNull(parkingLotId, "주차장 ID", ParkingLotErrorCode.INVALID_FORECAST);
		requireNonNull(targetAt, "예측 대상 시각", ParkingLotErrorCode.INVALID_FORECAST);
		validateRate(occupancyRate);
		return new OccupancyForecast(parkingLotId, targetAt, occupancyRate);
	}

	public void updateOccupancyRate(int occupancyRate) {
		validateRate(occupancyRate);
		this.occupancyRate = (short) occupancyRate;
	}

	public int getOccupancyRate() {
		return occupancyRate;
	}

	public CongestionLevel congestionLevel() {
		return CongestionLevel.of(occupancyRate);
	}

	private static void validateRate(int occupancyRate) {
		require(occupancyRate >= 0 && occupancyRate <= 100, "만차율은 0 ~ 100 사이여야 합니다", ParkingLotErrorCode.INVALID_FORECAST);
	}
}
