INSERT INTO parking_lots (
    name, location, address, total_spaces, fee_description, free_minutes,
    entrance_guide, indoor, weekend_only, fee_unit_minutes, fee_unit_price,
    created_at, updated_at
)
SELECT seed.name, ST_SetSRID(ST_MakePoint(seed.longitude, seed.latitude), 4326),
       seed.address, seed.total_spaces, seed.fee_description, seed.free_minutes,
       seed.entrance_guide, seed.indoor, seed.weekend_only,
       seed.fee_unit_minutes, seed.fee_unit_price, now(), now()
FROM (VALUES
    ('코엑스 주차장', 127.0598, 37.5116, '서울 강남구 영동대로 513 지하', 2000,
     '10분 800원', 30, '건물 뒷편 영동대로 쪽 입구, 봉은사로에서 우회전', TRUE, FALSE, 10, 800),
    ('봉은사 공영주차장', 127.0575, 37.5146, '서울 강남구 봉은사로 531', 300,
     '5분 400원', 0, '봉은사로에서 바로 진입, 넓음', FALSE, FALSE, 5, 400),
    ('롯데월드몰 주차장', 127.1040, 37.5131, '서울 송파구 올림픽로 300 지하', 2700,
     '10분 1,000원', 120, '올림픽로에서 진입, 입구 2곳', TRUE, FALSE, 10, 1000),
    ('잠실종합운동장 주차장', 127.0730, 37.5150, '서울 송파구 올림픽로 25', 1800,
     '경기일 1일 4,000원', 0, '동문 진입, 경기 종료 후 출차 정체 심함', FALSE, FALSE, NULL, NULL),
    ('서울역 서부 주차장', 126.9690, 37.5530, '서울 용산구 청파로 378', 600,
     '10분 600원', 0, '서부역 쪽 진입', FALSE, FALSE, 10, 600),
    ('강남역 공영주차장', 127.0290, 37.4965, '서울 강남구 테헤란로 지하', 350,
     '5분 400원', 0, '테헤란로 이면도로 진입, 일방통행', FALSE, FALSE, 5, 400),
    ('경복궁 주차장', 126.9750, 37.5770, '서울 종로구 사직로 161', 240,
     '최초 2시간 3,000원', 0, '사직로 서쪽 입구', FALSE, FALSE, NULL, NULL),
    ('DDP 지하주차장', 127.0105, 37.5660, '서울 중구 을지로 281 지하', 650,
     '10분 900원', 0, '을지로 방면에서 진입', TRUE, FALSE, 10, 900),
    ('IFC몰 주차장', 126.9265, 37.5245, '서울 영등포구 국제금융로 10 지하', 1800,
     '10분 1,000원', 60, '국제금융로 진입', TRUE, FALSE, 10, 1000),
    ('남산 공영주차장', 126.9900, 37.5540, '서울 중구 소파로', 300,
     '최초 1시간 2,000원', 0, '소파로 진입, 타워까지 도보 15분', FALSE, FALSE, NULL, NULL),
    ('현대백화점 판교점 주차장', 127.111991, 37.392799, '경기 성남시 분당구 판교역로146번길 20', NULL,
     '최초 30분 무료, 초과 10분당 1,000원, 구매 금액별 1~5시간 무료', 30, NULL, TRUE, FALSE, NULL, NULL),
    ('알파돔 임시 주차장', 127.110328, 37.394143, '경기 성남시 분당구 판교역로146번길 알파돔타워', NULL,
     '주말·공휴일 운영 시간 내 무료', 0, NULL, FALSE, TRUE, NULL, NULL),
    ('테크원 임시 주차장', 127.112183, 37.395417, '경기 성남시 분당구 분당내곡로 131 판교 테크원타워', NULL,
     '주말·공휴일 운영 시간 내 무료', 0, NULL, FALSE, TRUE, NULL, NULL)
) AS seed (name, longitude, latitude, address, total_spaces, fee_description, free_minutes,
           entrance_guide, indoor, weekend_only, fee_unit_minutes, fee_unit_price)
WHERE NOT EXISTS (SELECT 1 FROM parking_lots existing WHERE existing.name = seed.name);
