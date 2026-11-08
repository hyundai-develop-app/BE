package com.hmc.hmcbe.parkinglot.controller;

import static com.hmc.hmcbe.global.auth.AuthFixture.authenticated;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmc.hmcbe.global.geo.GeoPoint;
import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import com.hmc.hmcbe.parkinglot.dto.response.OccupancyForecastResponse;
import com.hmc.hmcbe.parkinglot.dto.response.ParkingLotResponse;
import com.hmc.hmcbe.parkinglot.service.ParkingLotReadService;
import com.hmc.hmcbe.user.service.UserWriteService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ParkingLotController.class)
class ParkingLotControllerTest {

	private static final Instant AT = Instant.parse("2026-11-09T05:18:00Z");
	private static final ParkingLotResponse LOT =
			new ParkingLotResponse(7L, "여의도 공영주차장", 37.5259, 126.9284, true, null, 120, 10, 1000, 300);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ParkingLotReadService parkingLotReadService;

	@MockitoBean
	private UserWriteService userWriteService;

	@Test
	@DisplayName("주차장을 조회한다")
	void getParkingLot() throws Exception {
		given(parkingLotReadService.getParkingLot(7L)).willReturn(LOT);

		mockMvc.perform(get("/api/v1/parking-lots/7").with(authenticated()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("여의도 공영주차장"))
				.andExpect(jsonPath("$.data.freeMinutes").value(120))
				.andExpect(jsonPath("$.data.feeUnitPrice").value(1000));
	}

	@Test
	@DisplayName("반경을 생략하면 500m로 주변 주차장을 조회한다")
	void findNearby() throws Exception {
		given(parkingLotReadService.findNearby(new GeoPoint(37.5259, 126.9284), 500, AT)).willReturn(List.of(
				new NearbyParkingLotResponse(LOT, 300, new OccupancyForecastResponse(AT, 40, "AVAILABLE"))));

		mockMvc.perform(get("/api/v1/parking-lots/nearby").with(authenticated())
						.param("latitude", "37.5259")
						.param("longitude", "126.9284")
						.param("at", AT.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].parkingLot.id").value(7))
				.andExpect(jsonPath("$.data[0].distanceMeters").value(300))
				.andExpect(jsonPath("$.data[0].forecast.congestionLevel").value("AVAILABLE"));
	}

	@Test
	@DisplayName("기준 시각이 없으면 400 INVALID_REQUEST")
	void findNearbyWithoutAt() throws Exception {
		mockMvc.perform(get("/api/v1/parking-lots/nearby").with(authenticated())
						.param("latitude", "37.5259")
						.param("longitude", "126.9284"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

		verifyNoInteractions(parkingLotReadService);
	}

	@Test
	@DisplayName("위도가 범위를 벗어나면 400 INVALID_REQUEST")
	void findNearbyWithInvalidLatitude() throws Exception {
		mockMvc.perform(get("/api/v1/parking-lots/nearby").with(authenticated())
						.param("latitude", "91")
						.param("longitude", "126.9284")
						.param("at", AT.toString()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

		verify(parkingLotReadService, never()).findNearby(any(), anyInt(), any());
	}

	@Test
	@DisplayName("예측 만차율을 구간으로 조회한다")
	void getForecasts() throws Exception {
		Instant from = Instant.parse("2026-11-09T04:30:00Z");
		Instant to = Instant.parse("2026-11-09T06:00:00Z");
		given(parkingLotReadService.getForecasts(7L, from, to)).willReturn(List.of(
				new OccupancyForecastResponse(from, 58, "AVAILABLE"),
				new OccupancyForecastResponse(to, 82, "NORMAL")));

		mockMvc.perform(get("/api/v1/parking-lots/7/forecasts").with(authenticated())
						.param("from", from.toString())
						.param("to", to.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[1].occupancyRate").value(82));
	}
}
