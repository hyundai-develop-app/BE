package com.hmc.hmcbe.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(PostgisContainerConfig.class)
@TestPropertySource(properties = "app.demo-seed.enabled=true")
@Transactional
class DemoSeedEnabledTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private Flyway flyway;

	@Test
	@DisplayName("데모 시드를 켜면 기본 마이그레이션 위치를 유지하고 반복 시드를 적용한다")
	void applyRepeatableSeedAlongsideSchemaMigrations() {
		// given / when
		List<String> locations = Arrays.stream(flyway.getConfiguration().getLocations())
				.map(Location::getDescriptor).toList();
		List<String> appliedScripts = Arrays.stream(flyway.info().applied())
				.map(MigrationInfo::getScript).toList();

		// then
		assertThat(locations).contains("classpath:db/migration", "classpath:db/demo");
		assertThat(appliedScripts).contains("R__demo_parking_lots.sql");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM parking_lot_occupancy_forecasts", Integer.class)).isZero();
	}

	@Test
	@DisplayName("데모 시드를 켜면 13곳의 좌표·주소·요금·입구·주말 전용 여부를 명세대로 적재한다")
	void seedAllThirteenParkingLotsWithMappedFields() {
		// given
		List<DemoSeedFixture.SeedLot> expected = DemoSeedFixture.expectedLots();

		// when
		List<DemoSeedFixture.SeedLot> actual = jdbcTemplate.query("""
				SELECT name, ST_Y(location) AS latitude, ST_X(location) AS longitude,
					address, total_spaces, fee_description, free_minutes, entrance_guide,
					indoor, weekend_only, fee_unit_minutes, fee_unit_price
				FROM parking_lots
				""", DemoSeedFixture::readLot);

		// then
		assertThat(actual).hasSize(13).containsExactlyInAnyOrderElementsOf(expected);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM parking_lots WHERE ST_SRID(location) <> 4326", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM parking_lots
				WHERE created_at IS NULL OR updated_at IS NULL OR created_at <> updated_at
				""", Integer.class)).isZero();
	}

	@Test
	@DisplayName("시드 SQL을 다시 실행하면 13곳의 ID와 값과 생성·수정 시각을 그대로 유지한다")
	void rerunningScriptDoesNotDuplicateOrChangeExistingRows() {
		// given
		List<Map<String, Object>> before = snapshot();
		assertThat(before).hasSize(13);

		// when
		executeSeedScript();

		// then
		assertThat(snapshot()).containsExactlyElementsOf(before);
	}

	@Test
	@DisplayName("동명 주차장과 별도 주차장이 있으면 기존 데이터를 보존하고 누락된 데모 주차장만 추가한다")
	void preserveExistingNamesAndInsertOnlyMissingLot() {
		// given
		jdbcTemplate.update("""
				UPDATE parking_lots
				SET address = '이미 등록된 주소', fee_unit_price = 1234,
					created_at = TIMESTAMPTZ '2020-01-01 00:00:00+00',
					updated_at = TIMESTAMPTZ '2021-01-01 00:00:00+00'
				WHERE name = '코엑스 주차장'
				""");
		jdbcTemplate.update("DELETE FROM parking_lots WHERE name = ?", "테크원 임시 주차장");
		jdbcTemplate.update("""
				INSERT INTO parking_lots (name, location, indoor, free_minutes, created_at, updated_at)
				VALUES ('별도로 등록된 주차장', ST_SetSRID(ST_MakePoint(127, 37), 4326),
					FALSE, 0, now(), now())
				""");
		List<Map<String, Object>> before = snapshot();
		assertThat(before).hasSize(13);

		// when
		executeSeedScript();

		// then
		assertThat(snapshot()).hasSize(14).containsAll(before);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM parking_lots WHERE name = ?", Integer.class, "테크원 임시 주차장"))
				.isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("""
				SELECT created_at = now() AND updated_at = now()
				FROM parking_lots WHERE name = ?
				""", Boolean.class, "테크원 임시 주차장")).isTrue();
	}

	private void executeSeedScript() {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
				new ClassPathResource("db/demo/R__demo_parking_lots.sql"));
		populator.setSqlScriptEncoding("UTF-8");
		populator.execute(dataSource);
	}

	private List<Map<String, Object>> snapshot() {
		return jdbcTemplate.queryForList("""
				SELECT id, name, ST_AsEWKT(location) AS location, address, indoor, entrance_guide,
					free_minutes, fee_unit_minutes, fee_unit_price, total_spaces,
					fee_description, weekend_only, created_at, updated_at
				FROM parking_lots ORDER BY name, id
				""");
	}
}
