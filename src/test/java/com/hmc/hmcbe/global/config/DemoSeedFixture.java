package com.hmc.hmcbe.global.config;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

final class DemoSeedFixture {

	private DemoSeedFixture() {
	}

	static List<SeedLot> expectedLots() {
		return List.of(
				new SeedLot("코엑스 주차장", 37.5116, 127.0598, "서울 강남구 영동대로 513 지하", 2000,
						"10분 800원", 30, "건물 뒷편 영동대로 쪽 입구, 봉은사로에서 우회전", true, false, 10, 800),
				new SeedLot("봉은사 공영주차장", 37.5146, 127.0575, "서울 강남구 봉은사로 531", 300,
						"5분 400원", 0, "봉은사로에서 바로 진입, 넓음", false, false, 5, 400),
				new SeedLot("롯데월드몰 주차장", 37.5131, 127.1040, "서울 송파구 올림픽로 300 지하", 2700,
						"10분 1,000원", 120, "올림픽로에서 진입, 입구 2곳", true, false, 10, 1000),
				new SeedLot("잠실종합운동장 주차장", 37.5150, 127.0730, "서울 송파구 올림픽로 25", 1800,
						"경기일 1일 4,000원", 0, "동문 진입, 경기 종료 후 출차 정체 심함", false, false, null, null),
				new SeedLot("서울역 서부 주차장", 37.5530, 126.9690, "서울 용산구 청파로 378", 600,
						"10분 600원", 0, "서부역 쪽 진입", false, false, 10, 600),
				new SeedLot("강남역 공영주차장", 37.4965, 127.0290, "서울 강남구 테헤란로 지하", 350,
						"5분 400원", 0, "테헤란로 이면도로 진입, 일방통행", false, false, 5, 400),
				new SeedLot("경복궁 주차장", 37.5770, 126.9750, "서울 종로구 사직로 161", 240,
						"최초 2시간 3,000원", 0, "사직로 서쪽 입구", false, false, null, null),
				new SeedLot("DDP 지하주차장", 37.5660, 127.0105, "서울 중구 을지로 281 지하", 650,
						"10분 900원", 0, "을지로 방면에서 진입", true, false, 10, 900),
				new SeedLot("IFC몰 주차장", 37.5245, 126.9265, "서울 영등포구 국제금융로 10 지하", 1800,
						"10분 1,000원", 60, "국제금융로 진입", true, false, 10, 1000),
				new SeedLot("남산 공영주차장", 37.5540, 126.9900, "서울 중구 소파로", 300,
						"최초 1시간 2,000원", 0, "소파로 진입, 타워까지 도보 15분", false, false, null, null),
				new SeedLot("현대백화점 판교점 주차장", 37.392799, 127.111991,
						"경기 성남시 분당구 판교역로146번길 20", null,
						"최초 30분 무료, 초과 10분당 1,000원, 구매 금액별 1~5시간 무료", 30, null,
						true, false, null, null),
				new SeedLot("알파돔 임시 주차장", 37.394143, 127.110328,
						"경기 성남시 분당구 판교역로146번길 알파돔타워", null,
						"주말·공휴일 운영 시간 내 무료", 0, null, false, true, null, null),
				new SeedLot("테크원 임시 주차장", 37.395417, 127.112183,
						"경기 성남시 분당구 분당내곡로 131 판교 테크원타워", null,
						"주말·공휴일 운영 시간 내 무료", 0, null, false, true, null, null));
	}

	static SeedLot readLot(ResultSet row, int rowNumber) throws SQLException {
		return new SeedLot(row.getString("name"), row.getDouble("latitude"), row.getDouble("longitude"),
				row.getString("address"), row.getObject("total_spaces", Integer.class),
				row.getString("fee_description"), row.getInt("free_minutes"), row.getString("entrance_guide"),
				row.getBoolean("indoor"), row.getBoolean("weekend_only"),
				row.getObject("fee_unit_minutes", Integer.class), row.getObject("fee_unit_price", Integer.class));
	}

	record SeedLot(String name, double latitude, double longitude, String address, Integer totalSpaces,
			String feeDescription, int freeMinutes, String entranceGuide, boolean indoor, boolean weekendOnly,
			Integer feeUnitMinutes, Integer feeUnitPrice) {
	}
}
