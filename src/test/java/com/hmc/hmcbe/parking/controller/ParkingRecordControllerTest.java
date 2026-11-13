package com.hmc.hmcbe.parking.controller;

import static com.hmc.hmcbe.global.auth.AuthFixture.USER_ID;
import static com.hmc.hmcbe.global.auth.AuthFixture.authenticated;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmc.hmcbe.global.auth.AuthFixture;
import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.parking.dto.request.ParkRequest;
import com.hmc.hmcbe.parking.dto.response.ParkingRecordResponse;
import com.hmc.hmcbe.parking.dto.response.ParkingSummaryResponse;
import com.hmc.hmcbe.parking.exception.ParkingErrorCode;
import com.hmc.hmcbe.parking.service.ParkingRecordReadService;
import com.hmc.hmcbe.parking.service.ParkingRecordWriteService;
import com.hmc.hmcbe.place.dto.response.SavedPlaceResponse;
import com.hmc.hmcbe.user.service.UserWriteService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ParkingRecordController.class)
class ParkingRecordControllerTest {

	private static final Instant PARKED_AT = Instant.parse("2026-11-09T05:20:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ParkingRecordReadService parkingRecordReadService;

	@MockitoBean
	private ParkingRecordWriteService parkingRecordWriteService;

	@MockitoBean
	private UserWriteService userWriteService;

	@BeforeEach
	void setUp() {
		given(userWriteService.resolveUserId(AuthFixture.SUBJECT)).willReturn(USER_ID);
	}

	@Test
	@DisplayName("P단 주차를 기록하면 201로 기록을 반환한다")
	void park() throws Exception {
		given(parkingRecordWriteService.park(USER_ID, new ParkRequest(37.5262, 126.929, 7L)))
				.willReturn(record(null));

		mockMvc.perform(post("/api/v1/parking-records").with(authenticated())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"latitude\":37.5262,\"longitude\":126.929,\"parkingLotId\":7}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.id").value(3))
				.andExpect(jsonPath("$.data.parkedAt").value("2026-11-09T05:20:00Z"));
	}

	@Test
	@DisplayName("주차장 ID를 생략하면 GPS 요청을 전달하고 추정된 주차장 ID를 반환한다")
	void parkWithoutParkingLotId() throws Exception {
		// given
		ParkRequest request = new ParkRequest(37.5262, 126.929, null);
		given(parkingRecordWriteService.park(USER_ID, request)).willReturn(record(null));

		// when & then
		mockMvc.perform(post("/api/v1/parking-records").with(authenticated())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"latitude\":37.5262,\"longitude\":126.929}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.parkingLotId").value(7));
		verify(parkingRecordWriteService).park(USER_ID, request);
	}

	@Test
	@DisplayName("위치가 없으면 400 INVALID_REQUEST")
	void parkWithoutLocation() throws Exception {
		mockMvc.perform(post("/api/v1/parking-records").with(authenticated())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"parkingLotId\":7}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("COMMON4001"));

		verifyNoInteractions(parkingRecordWriteService);
	}

	@Test
	@DisplayName("층·구역을 저장한다")
	void updateSpotMemo() throws Exception {
		given(parkingRecordWriteService.updateSpotMemo(USER_ID, 3L, "지하 2층 C구역"))
				.willReturn(record("지하 2층 C구역"));

		mockMvc.perform(put("/api/v1/parking-records/3/spot-memo").with(authenticated())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"spotMemo\":\"지하 2층 C구역\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.spotMemo").value("지하 2층 C구역"));
	}

	@Test
	@DisplayName("빈 층·구역은 400")
	void updateBlankSpotMemo() throws Exception {
		mockMvc.perform(put("/api/v1/parking-records/3/spot-memo").with(authenticated())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"spotMemo\":\" \"}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(parkingRecordWriteService);
	}

	@Test
	@DisplayName("다음 시동 요약을 조회한다")
	void getSummary() throws Exception {
		// given
		given(parkingRecordReadService.getSummary(USER_ID)).willReturn(new ParkingSummaryResponse(
				record("지하 2층 C구역"), "여의도 공영주차장", "서울 강남구 영동대로 513 지하", "10분 800원", 100, 120, 20L,
				List.of(new SavedPlaceResponse(1L, "집", 37.49, 127.02))));

		// when & then
		mockMvc.perform(get("/api/v1/parking-records/active/summary").with(authenticated()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.parkingLotName").value("여의도 공영주차장"))
				.andExpect(jsonPath("$.data.parkingLotAddress").value("서울 강남구 영동대로 513 지하"))
				.andExpect(jsonPath("$.data.feeDescription").value("10분 800원"))
				.andExpect(jsonPath("$.data.elapsedMinutes").value(100))
				.andExpect(jsonPath("$.data.freeMinutes").value(120))
				.andExpect(jsonPath("$.data.remainingFreeMinutes").value(20))
				.andExpect(jsonPath("$.data.quickDestinations[0].name").value("집"));
	}

	@Test
	@DisplayName("주차장을 모르면 요약 JSON에 주소와 요금 문구를 null로 포함한다")
	void getSummaryWithoutParkingLot() throws Exception {
		// given
		ParkingRecordResponse record = new ParkingRecordResponse(3L, null, 37.5262, 126.929, PARKED_AT, null, null);
		given(parkingRecordReadService.getSummary(USER_ID)).willReturn(new ParkingSummaryResponse(
				record, null, null, null, 100, null, null, List.of()));

		// when & then
		mockMvc.perform(get("/api/v1/parking-records/active/summary").with(authenticated()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data", hasKey("parkingLotAddress")))
				.andExpect(jsonPath("$.data.parkingLotAddress").value(nullValue()))
				.andExpect(jsonPath("$.data", hasKey("feeDescription")))
				.andExpect(jsonPath("$.data.feeDescription").value(nullValue()))
				.andExpect(jsonPath("$.data.elapsedMinutes").value(100));
	}

	@Test
	@DisplayName("진행 중인 기록이 없으면 404")
	void noActiveRecord() throws Exception {
		given(parkingRecordReadService.getActiveRecord(USER_ID))
				.willThrow(new BusinessException(ParkingErrorCode.ACTIVE_PARKING_RECORD_NOT_FOUND));

		mockMvc.perform(get("/api/v1/parking-records/active").with(authenticated()))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("출차한다")
	void exit() throws Exception {
		mockMvc.perform(post("/api/v1/parking-records/3/exit").with(authenticated()))
				.andExpect(status().isOk());

		verify(parkingRecordWriteService).exit(USER_ID, 3L);
	}

	@Test
	@DisplayName("이미 출차한 기록을 다시 출차하면 409")
	void exitTwice() throws Exception {
		willThrow(new BusinessException(ParkingErrorCode.ALREADY_EXITED)).given(parkingRecordWriteService).exit(USER_ID, 3L);

		mockMvc.perform(post("/api/v1/parking-records/3/exit").with(authenticated()))
				.andExpect(status().isConflict());
	}

	private static ParkingRecordResponse record(String spotMemo) {
		return new ParkingRecordResponse(3L, 7L, 37.5262, 126.929, PARKED_AT, spotMemo, null);
	}
}
