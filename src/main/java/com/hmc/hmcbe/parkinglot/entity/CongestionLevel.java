package com.hmc.hmcbe.parkinglot.entity;

/** 예측 만차율에서 계산하는 혼잡도 등급. 화면의 "여유"/"혼잡 예상" 칩에 대응한다. */
public enum CongestionLevel {

	AVAILABLE,
	NORMAL,
	CONGESTED;

	private static final int NORMAL_THRESHOLD = 70;
	private static final int CONGESTED_THRESHOLD = 90;

	public static CongestionLevel of(int occupancyRate) {
		if (occupancyRate >= CONGESTED_THRESHOLD) {
			return CONGESTED;
		}
		if (occupancyRate >= NORMAL_THRESHOLD) {
			return NORMAL;
		}
		return AVAILABLE;
	}
}
