package com.hmc.hmcbe.place.entity;

import static com.hmc.hmcbe.global.exception.DomainPreconditions.requireNonNull;
import static com.hmc.hmcbe.global.exception.DomainPreconditions.requireText;

import com.hmc.hmcbe.global.entity.BaseTimeEntity;
import com.hmc.hmcbe.global.geo.GeoPoint;
import com.hmc.hmcbe.place.exception.PlaceErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

/** 다음 시동 요약의 "어디로 갈까요?" 빠른 목적지(집, 회사 등). */
@Getter
@Entity
@Table(name = "saved_places")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedPlace extends BaseTimeEntity {

	private static final int MAX_NAME_LENGTH = 50;

	@Column(nullable = false, updatable = false)
	private Long userId;

	@Column(nullable = false, length = MAX_NAME_LENGTH)
	private String name;

	@Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
	private Point location;

	private SavedPlace(Long userId, String name, Point location) {
		this.userId = userId;
		this.name = name;
		this.location = location;
	}

	public static SavedPlace create(Long userId, String name, GeoPoint location) {
		requireNonNull(userId, "사용자 ID", PlaceErrorCode.INVALID_SAVED_PLACE);
		requireText(name, MAX_NAME_LENGTH, "장소 이름", PlaceErrorCode.INVALID_SAVED_PLACE);
		requireNonNull(location, "장소 위치", PlaceErrorCode.INVALID_SAVED_PLACE);
		return new SavedPlace(userId, name, location.toPoint());
	}

	public boolean isOwnedBy(Long userId) {
		return this.userId.equals(userId);
	}

	public GeoPoint getLocation() {
		return GeoPoint.from(location);
	}
}
