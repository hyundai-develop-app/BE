package com.hmc.hmcbe.parkinglot.entity;

import static com.hmc.hmcbe.global.exception.DomainPreconditions.require;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** "10분당 1,000원" 형태의 단위 요금. */
@Embeddable
public record ParkingFee(
		@Column(name = "fee_unit_minutes") int unitMinutes,
		@Column(name = "fee_unit_price") int unitPrice
) {

	public ParkingFee {
		require(unitMinutes > 0, "요금 단위 시간은 1분 이상이어야 합니다");
		require(unitPrice >= 0, "단위 요금은 0원 이상이어야 합니다");
	}
}
