package com.hmc.hmcbe.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@MappedSuperclass
public abstract class BaseTimeEntity extends BaseCreatedEntity {

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;
}
