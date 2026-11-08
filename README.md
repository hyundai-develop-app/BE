# HMC-BE
HMC 백엔드

## 로컬 실행

Java 17과 Docker가 필요합니다. `.env.example`을 `.env`로 복사해 개발 DB 설정을 채우고 실행합니다.

```sh
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

DB 스키마는 Flyway(`src/main/resources/db/migration`)로 관리하며 Hibernate는 검증만 합니다.

## 검증

```sh
./gradlew check
```

Repository 테스트는 Testcontainers로 격리된 PostgreSQL/PostGIS를 사용하므로 Docker가 실행 중이어야 합니다. 로컬 DB, 외부 인증, S3, Firebase에 의존하지 않습니다.

iCloud 등 동기화 폴더에서 빌드 산출물이 복제되거나 파일 읽기가 지연되면 산출물 경로를 폴더 밖으로 지정할 수 있습니다. 기본 경로는 기존처럼 `build/`입니다. ArchUnit이 테스트 클래스를 구분할 수 있도록 외부 경로도 마지막 디렉터리 이름을 `build`로 지정합니다.

```sh
./gradlew check -PhmcBuildDir=/tmp/hmc-verification/build
```
