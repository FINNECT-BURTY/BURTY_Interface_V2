-- 일일 이체 사용량에 낙관적 잠금 버전 컬럼 추가
--
-- 한도 검사는 "읽고 → 검사하고 → 더한다" 라서 동시 요청에 취약하다.
-- 조건부 UPDATE 나 SELECT ... FOR UPDATE 는 DB 엔진의 MVCC/격리 수준 구현에 따라 결과가 달라진다.
-- @Version 은 JPA 가 UPDATE 의 WHERE 에 버전을 넣고 영향 행 수를 확인하는 방식이라
-- 엔진과 무관하게 lost update 를 잡아낸다.

ALTER TABLE tbl_daily_transfer_usage
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
