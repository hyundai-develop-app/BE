package com.hmc.hmcbe.arrival.controller;

import static com.hmc.hmcbe.global.auth.AuthFixture.USER_ID;
import static com.hmc.hmcbe.global.auth.AuthFixture.authenticated;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmc.hmcbe.arrival.dto.request.StartArrivalRequest;
import com.hmc.hmcbe.arrival.dto.response.ArrivalOutlookResponse;
import com.hmc.hmcbe.arrival.dto.response.ArrivalSessionResponse;
import com.hmc.hmcbe.arrival.dto.response.ParkingBriefingResponse;
import com.hmc.hmcbe.arrival.entity.ArrivalStatus;
import com.hmc.hmcbe.arrival.exception.ArrivalErrorCode;
import com.hmc.hmcbe.arrival.service.ArrivalReadService;
import com.hmc.hmcbe.arrival.service.ArrivalWriteService;
import com.hmc.hmcbe.global.auth.AuthFixture;
import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import com.hmc.hmcbe.parkinglot.dto.response.OccupancyForecastResponse;
import com.hmc.hmcbe.parkinglot.dto.response.ParkingLotResponse;
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

@WebMvcTest(ArrivalController.class)
class ArrivalControllerTest {

	private static final Instant ETA = Instant.parse("2026-11-09T05:18:00Z");
	private static final ArrivalSessionResponse SESSION = new ArrivalSessionResponse(
			5L, "더현대 서울", 37.5259, 126.9284, 7L, null, ETA, ArrivalStatus.ACTIVE);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ArrivalReadService arrivalReadService;

	@MockitoBean
	private ArrivalWriteService arrivalWriteService;

	@MockitoBean
	private UserWriteService userWriteService;

	@BeforeEach
	void setUp() {
		given(userWriteService.resolveUserId(AuthFixture.SUBJECT)).willReturn(USER_ID);
	}

	@Test
	@DisplayName("도착 모드를 시작하면 201로 세션을 반환한다")
	void start() throws Exception {
		StartArrivalRequest request = new StartArrivalRequest("더현대 서울", 37.5259, 126.9284, 7L, ETA);
		given(arrivalWriteService.start(USER_ID, request)).willReturn(SESSION);

		mockMvc.perform(post("/api/v1/arrivals").with(authenticated())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"destinationName":"더현대 서울","latitude":37.5259,"longitude":126.9284,
								"destinationParkingLotId":7,"expectedArrivalAt":"2026-11-09T05:18:00Z"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.id").value(5))
				.andExpect(jsonPath("$.data.status").value("ACTIVE"))
				.andExpect(jsonPath("$.data.expectedArrivalAt").value("2026-11-09T05:18:00Z"));
	}

	@Test
	@DisplayName("예상 도착 시각이 없으면 400 INVALID_REQUEST")
	void startWithoutEta() throws Exception {
		mockMvc.perform(post("/api/v1/arrivals").with(authenticated())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"destinationName\":\"더현대 서울\",\"latitude\":37.5,\"longitude\":126.9}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("COMMON4001"));

		verifyNoInteractions(arrivalWriteService);
	}

	@Test
	@DisplayName("진행 중인 세션이 없으면 404")
	void noActiveSession() throws Exception {
		given(arrivalReadService.getActiveSession(USER_ID))
				.willThrow(new BusinessException(ArrivalErrorCode.ACTIVE_ARRIVAL_SESSION_NOT_FOUND));

		mockMvc.perform(get("/api/v1/arrivals/active").with(authenticated()))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("대기 화면 데이터를 조회한다")
	void getOutlook() throws Exception {
		given(arrivalReadService.getOutlook(USER_ID, 5L)).willReturn(new ArrivalOutlookResponse(
				SESSION, nearby(7L, 0, 95, "CONGESTED"),
				List.of(new OccupancyForecastResponse(ETA, 95, "CONGESTED")), 2));

		mockMvc.perform(get("/api/v1/arrivals/5/outlook").with(authenticated()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.destinationParkingLot.forecast.occupancyRate").value(95))
				.andExpect(jsonPath("$.data.destinationForecasts.length()").value(1))
				.andExpect(jsonPath("$.data.betterAlternativeCount").value(2));
	}

	@Test
	@DisplayName("브리핑을 조회한다")
	void getBriefing() throws Exception {
		NearbyParkingLotResponse recommended = nearby(3L, 300, 40, "AVAILABLE");
		given(arrivalReadService.getBriefing(USER_ID, 5L)).willReturn(new ParkingBriefingResponse(
				nearby(7L, 0, 95, "CONGESTED"), recommended, List.of(recommended)));

		mockMvc.perform(get("/api/v1/arrivals/5/briefing").with(authenticated()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.recommended.parkingLot.id").value(3))
				.andExpect(jsonPath("$.data.recommended.distanceMeters").value(300))
				.andExpect(jsonPath("$.data.destinationParkingLot.parkingLot.id").value(7));
	}

	@Test
	@DisplayName("경유 주차장을 선택한다")
	void selectWaypoint() throws Exception {
		given(arrivalWriteService.selectWaypoint(USER_ID, 5L, 3L)).willReturn(new ArrivalSessionResponse(
				5L, "더현대 서울", 37.5259, 126.9284, 7L, 3L, ETA, ArrivalStatus.ACTIVE));

		mockMvc.perform(put("/api/v1/arrivals/5/waypoint").with(authenticated())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"parkingLotId\":3}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.waypointParkingLotId").value(3));
	}

	@Test
	@DisplayName("경유 주차장 ID가 없으면 400")
	void selectWaypointWithoutId() throws Exception {
		mockMvc.perform(put("/api/v1/arrivals/5/waypoint").with(authenticated())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(arrivalWriteService);
	}

	@Test
	@DisplayName("도착 모드를 완료한다")
	void complete() throws Exception {
		mockMvc.perform(post("/api/v1/arrivals/5/complete").with(authenticated()))
				.andExpect(status().isOk());

		verify(arrivalWriteService).complete(USER_ID, 5L);
	}

	@Test
	@DisplayName("이미 끝난 세션을 취소하면 409 CONFLICT")
	void cancelEnded() throws Exception {
		willThrow(new BusinessException(ArrivalErrorCode.ALREADY_ENDED)).given(arrivalWriteService).cancel(USER_ID, 5L);

		mockMvc.perform(post("/api/v1/arrivals/5/cancel").with(authenticated()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("ARRIVAL4091"));
	}

	private static NearbyParkingLotResponse nearby(Long id, int distance, int rate, String level) {
		ParkingLotResponse lot = new ParkingLotResponse(id, "주차장" + id, 37.5, 126.9, true, null, 120, 10, 1000, 100, null, null, false);
		return new NearbyParkingLotResponse(lot, distance, new OccupancyForecastResponse(ETA, rate, level));
	}

	@Test
	@DisplayName("경유지 해제를 요청하면 200으로 경유지가 null인 세션을 반환한다")
	void clearWaypoint() throws Exception {
		// given
		given(arrivalWriteService.clearWaypoint(USER_ID, 5L)).willReturn(SESSION);

		// when & then
		mockMvc.perform(delete("/api/v1/arrivals/5/waypoint").with(authenticated()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.error").value(nullValue()))
				.andExpect(jsonPath("$.data.id").value(5))
				.andExpect(jsonPath("$.data.waypointParkingLotId").value(nullValue()))
				.andExpect(jsonPath("$.data.destinationParkingLotId").value(7))
				.andExpect(jsonPath("$.data.status").value("ACTIVE"));
		verify(arrivalWriteService).clearWaypoint(USER_ID, 5L);
	}

	@Test
	@DisplayName("남의 세션이거나 없는 세션의 경유지 해제를 요청하면 404 ARRIVAL4041을 반환한다")
	void clearWaypointNotFound() throws Exception {
		// given
		given(arrivalWriteService.clearWaypoint(USER_ID, 5L))
				.willThrow(new BusinessException(ArrivalErrorCode.ARRIVAL_SESSION_NOT_FOUND));

		// when & then
		mockMvc.perform(delete("/api/v1/arrivals/5/waypoint").with(authenticated()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("ARRIVAL4041"));
		verify(arrivalWriteService).clearWaypoint(USER_ID, 5L);
	}

	@Test
	@DisplayName("종료한 세션의 경유지 해제를 요청하면 409 ARRIVAL4091을 반환한다")
	void clearWaypointAlreadyEnded() throws Exception {
		// given
		given(arrivalWriteService.clearWaypoint(USER_ID, 5L))
				.willThrow(new BusinessException(ArrivalErrorCode.ALREADY_ENDED));

		// when & then
		mockMvc.perform(delete("/api/v1/arrivals/5/waypoint").with(authenticated()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("ARRIVAL4091"));
		verify(arrivalWriteService).clearWaypoint(USER_ID, 5L);
	}

	@Test
	@DisplayName("목적지 주차장 ID를 생략하여 시작하면 추정된 ID가 담긴 201 응답을 반환한다")
	void startWithoutDestinationParkingLotId() throws Exception {
		// given
		StartArrivalRequest request = new StartArrivalRequest("더현대 서울", 37.5259, 126.9284, null, ETA);
		given(arrivalWriteService.start(USER_ID, request)).willReturn(SESSION);

		// when & then
		mockMvc.perform(post("/api/v1/arrivals").with(authenticated())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"destinationName":"더현대 서울","latitude":37.5259,"longitude":126.9284,
								"expectedArrivalAt":"2026-11-09T05:18:00Z"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.destinationParkingLotId").value(7));
		verify(arrivalWriteService).start(USER_ID, request);
	}
}
