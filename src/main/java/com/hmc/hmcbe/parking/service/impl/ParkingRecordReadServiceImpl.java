package com.hmc.hmcbe.parking.service.impl;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.parking.dto.response.ParkingRecordResponse;
import com.hmc.hmcbe.parking.dto.response.ParkingSummaryResponse;
import com.hmc.hmcbe.parking.entity.ParkingRecord;
import com.hmc.hmcbe.parking.exception.ParkingErrorCode;
import com.hmc.hmcbe.parking.repository.ParkingRecordRepository;
import com.hmc.hmcbe.parking.service.ParkingRecordReadService;
import com.hmc.hmcbe.parkinglot.dto.response.ParkingLotResponse;
import com.hmc.hmcbe.parkinglot.service.ParkingLotReadService;
import com.hmc.hmcbe.place.service.SavedPlaceReadService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParkingRecordReadServiceImpl implements ParkingRecordReadService {

	private final ParkingRecordRepository parkingRecordRepository;
	private final ParkingLotReadService parkingLotReadService;
	private final SavedPlaceReadService savedPlaceReadService;
	private final Clock clock;

	@Override
	public ParkingRecordResponse getActiveRecord(Long userId) {
		return ParkingRecordResponse.from(findActiveRecordOrThrow(userId));
	}

	@Override
	public ParkingSummaryResponse getSummary(Long userId) {
		ParkingRecord record = findActiveRecordOrThrow(userId);
		Instant now = Instant.now(clock);
		ParkingLotResponse lot = record.getParkingLotId() == null
				? null
				: parkingLotReadService.getParkingLot(record.getParkingLotId());
		return new ParkingSummaryResponse(
				ParkingRecordResponse.from(record),
				lot == null ? null : lot.name(),
				lot == null ? null : lot.address(),
				lot == null ? null : lot.feeDescription(),
				record.elapsed(now).toMinutes(),
				lot == null ? null : lot.freeMinutes(),
				lot == null ? null : record.remainingFreeTime(Duration.ofMinutes(lot.freeMinutes()), now).toMinutes(),
				savedPlaceReadService.getSavedPlaces(userId));
	}

	private ParkingRecord findActiveRecordOrThrow(Long userId) {
		return parkingRecordRepository.findByUserIdAndExitedAtIsNull(userId)
				.orElseThrow(() -> new BusinessException(ParkingErrorCode.ACTIVE_PARKING_RECORD_NOT_FOUND));
	}
}
