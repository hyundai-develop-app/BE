package com.hmc.hmcbe.parkinglot.dto.response;

import com.hmc.hmcbe.parkinglot.entity.ParkingFee;
import com.hmc.hmcbe.parkinglot.entity.ParkingLot;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주차장 정보")
public record ParkingLotResponse(
		@Schema(description = "주차장 ID", example = "1")
		Long id,
		@Schema(description = "주차장 이름", example = "스타필드 하남 주차장")
		String name,
		@Schema(description = "위도", example = "37.5453")
		double latitude,
		@Schema(description = "경도", example = "127.2236")
		double longitude,
		@Schema(description = "실내(지하·건물) 주차장 여부", example = "true")
		boolean indoor,
		@Schema(description = "입구 안내 문구. 없으면 null", example = "정문 지하 진입로 이용", nullable = true)
		String entranceGuide,
		@Schema(description = "무료 시간(분). 0이면 무료 시간 없음", example = "180")
		int freeMinutes,
		@Schema(description = "요금 단위 시간(분). 예: 10분당 1,000원의 10. 요금 정보가 없으면 null", example = "10", nullable = true)
		Integer feeUnitMinutes,
		@Schema(description = "단위 요금(원). 요금 정보가 없으면 null", example = "1000", nullable = true)
		Integer feeUnitPrice,
		@Schema(description = "총 주차면 수. 모르면 null", example = "6000", nullable = true)
		Integer totalSpaces,
		@Schema(description = "주소. 모르면 null", example = "서울 강남구 영동대로 513 지하", nullable = true)
		String address,
		@Schema(description = "요금 안내 문구(앱이 그대로 읽어 준다). 모르면 null", example = "10분 800원", nullable = true)
		String feeDescription,
		@Schema(description = "토·일에만 운영(공휴일은 판단하지 않음). 평일 주변 검색에서는 빠진다", example = "false")
		boolean weekendOnly
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
				lot.getTotalSpaces(),
				lot.getAddress(),
				lot.getFeeDescription(),
				lot.isWeekendOnly());
	}
}
