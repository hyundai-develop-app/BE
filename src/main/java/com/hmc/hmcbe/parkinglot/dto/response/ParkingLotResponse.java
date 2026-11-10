package com.hmc.hmcbe.parkinglot.dto.response;

import com.hmc.hmcbe.parkinglot.entity.ParkingFee;
import com.hmc.hmcbe.parkinglot.entity.ParkingLot;

public record ParkingLotResponse(
		Long id,
		String name,
		double latitude,
		double longitude,
		boolean indoor,
		String entranceGuide,
		int freeMinutes,
		Integer feeUnitMinutes,
		Integer feeUnitPrice,
		Integer totalSpaces
) {

	public static ParkingLotResponse from(ParkingLot lot) {
		ParkingFee fee = lot.getFee();
		return new ParkingLotResponse(
				lot.getId(),
				lot.getName(),
				lot.getLocation().latitude(),
				lot.getLocation().longitude(),
				lot.isIndoor(),
				lot.getEntranceGuide(),
				lot.getFreeMinutes(),
				fee == null ? null : fee.unitMinutes(),
				fee == null ? null : fee.unitPrice(),
				lot.getTotalSpaces());
	}
}
