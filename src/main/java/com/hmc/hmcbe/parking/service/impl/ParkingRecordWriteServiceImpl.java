package com.hmc.hmcbe.parking.service.impl;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.exception.ErrorCode;
import com.hmc.hmcbe.global.geo.GeoPoint;
import com.hmc.hmcbe.parking.dto.request.ParkRequest;
import com.hmc.hmcbe.parking.dto.response.ParkingRecordResponse;
import com.hmc.hmcbe.parking.entity.ParkingRecord;
import com.hmc.hmcbe.parking.repository.ParkingRecordRepository;
import com.hmc.hmcbe.parking.service.ParkingRecordWriteService;
import com.hmc.hmcbe.parkinglot.service.ParkingLotReadService;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ParkingRecordWriteServiceImpl implements ParkingRecordWriteService {

	private final ParkingRecordRepository parkingRecordRepository;
	private final ParkingLotReadService parkingLotReadService;
	private final Clock clock;

	@Override
	public ParkingRecordResponse park(Long userId, ParkRequest request) {
		if (request.parkingLotId() != null) {
			parkingLotReadService.getParkingLot(request.parkingLotId());
		}
		Instant now = Instant.now(clock);
		exitActiveRecord(userId, now);
		ParkingRecord record = ParkingRecord.record(userId, request.parkingLotId(),
				new GeoPoint(request.latitude(), request.longitude()), now);
		return ParkingRecordResponse.from(parkingRecordRepository.save(record));
	}

	@Override
	public ParkingRecordResponse updateSpotMemo(Long userId, Long recordId, String spotMemo) {
		ParkingRecord record = findOwnedRecordOrThrow(userId, recordId);
		record.updateSpotMemo(spotMemo);
		return ParkingRecordResponse.from(record);
	}

	@Override
	public void exit(Long userId, Long recordId) {
		findOwnedRecordOrThrow(userId, recordId).exit(Instant.now(clock));
	}

	/**
	 * 출차 신호를 놓친 이전 기록을 정리한다. Hibernate는 INSERT를 UPDATE보다 먼저 flush하므로
	 * "사용자당 진행 중 기록 하나" 부분 유니크 인덱스에 걸리지 않게 먼저 flush한다.
	 */
	private void exitActiveRecord(Long userId, Instant now) {
		parkingRecordRepository.findByUserIdAndExitedAtIsNull(userId)
				.ifPresent(active -> {
					active.exit(now);
					parkingRecordRepository.flush();
				});
	}

	/** 다른 사용자의 기록은 존재 여부를 드러내지 않도록 NOT_FOUND로 응답한다. */
	private ParkingRecord findOwnedRecordOrThrow(Long userId, Long recordId) {
		return parkingRecordRepository.findById(recordId)
				.filter(record -> record.isOwnedBy(userId))
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "주차 기록을 찾을 수 없습니다"));
	}
}
