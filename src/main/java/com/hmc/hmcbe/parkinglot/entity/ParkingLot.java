package com.hmc.hmcbe.parkinglot.entity;

import static com.hmc.hmcbe.global.exception.DomainPreconditions.require;
import static com.hmc.hmcbe.global.exception.DomainPreconditions.requireMaxLength;
import static com.hmc.hmcbe.global.exception.DomainPreconditions.requireNonNull;
import static com.hmc.hmcbe.global.exception.DomainPreconditions.requireText;

import com.hmc.hmcbe.global.entity.BaseTimeEntity;
import com.hmc.hmcbe.global.geo.GeoPoint;
import com.hmc.hmcbe.parkinglot.exception.ParkingLotErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Getter
@Entity
@Table(name = "parking_lots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParkingLot extends BaseTimeEntity {

	private static final int MAX_NAME_LENGTH = 100;
	private static final int MAX_ENTRANCE_GUIDE_LENGTH = 255;

	@Column(nullable = false, length = MAX_NAME_LENGTH)
	private String name;

	@Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
	private Point location;

	@Column(nullable = false)
	private boolean indoor;

	@Column(length = MAX_ENTRANCE_GUIDE_LENGTH)
	private String entranceGuide;

	@Column(nullable = false)
	private int freeMinutes;

	@Embedded
	private ParkingFee fee;

	private Integer totalSpaces;

	private ParkingLot(String name, Point location, boolean indoor, String entranceGuide, int freeMinutes,
			ParkingFee fee, Integer totalSpaces) {
		this.name = name;
		this.location = location;
		this.indoor = indoor;
		this.entranceGuide = entranceGuide;
		this.freeMinutes = freeMinutes;
		this.fee = fee;
		this.totalSpaces = totalSpaces;
	}

	public static ParkingLot create(String name, GeoPoint location, boolean indoor, String entranceGuide,
			int freeMinutes, ParkingFee fee, Integer totalSpaces) {
		requireText(name, MAX_NAME_LENGTH, "주차장 이름", ParkingLotErrorCode.INVALID_PARKING_LOT);
		requireNonNull(location, "주차장 위치", ParkingLotErrorCode.INVALID_PARKING_LOT);
		requireMaxLength(entranceGuide, MAX_ENTRANCE_GUIDE_LENGTH, "입구 안내", ParkingLotErrorCode.INVALID_PARKING_LOT);
		require(freeMinutes >= 0, "무료 시간은 0분 이상이어야 합니다", ParkingLotErrorCode.INVALID_PARKING_LOT);
		require(totalSpaces == null || totalSpaces > 0, "총 면수는 1 이상이어야 합니다", ParkingLotErrorCode.INVALID_PARKING_LOT);
		return new ParkingLot(name, location.toPoint(), indoor, entranceGuide, freeMinutes, fee, totalSpaces);
	}

	public GeoPoint getLocation() {
		return GeoPoint.from(location);
	}
}
