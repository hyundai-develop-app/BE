-- 주변 검색의 ST_DWithin(location::geography, ...)와 일치하는 표현식 인덱스.
CREATE INDEX idx_parking_lots_location_geography ON parking_lots USING GIST ((location::geography));
