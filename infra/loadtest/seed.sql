-- 부하 시험용 표본 데이터.
--
-- 빈 DB 를 대상으로 부하를 걸면 조회가 항상 빈 결과를 돌려주고, 인덱스도 옵티마이저도
-- 실제와 다르게 동작한다. 그런 측정은 아무것도 말해주지 않는다.
-- 거래내역은 사용자당 수만 건까지 쌓이는 테이블이므로 그 규모로 채운다.

SET @rows = COALESCE(@rows, 50000);

INSERT INTO tbl_user (ci_hash, ci, phone_hash, phone, status, failed_login_count, created_at, updated_at)
VALUES (REPEAT('1', 64), 'load-test-ci', REPEAT('2', 64), '010-1111-2222', 'ACTIVE', 0, NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE updated_at = NOW(6);

SET @uid = (SELECT user_id FROM tbl_user WHERE ci_hash = REPEAT('1', 64));

-- 재귀 CTE 로 한 번에 생성한다. 행마다 INSERT 를 돌리면 시딩 자체가 몇 분씩 걸린다.
SET SESSION max_recursive_iterations = 1000000;

INSERT IGNORE INTO tbl_transaction
  (user_id, account_id, external_tx_id, txn_date, amount, direction, source,
   merchant, expense_category_code, created_at)
WITH RECURSIVE seq(n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < @rows
)
SELECT
  @uid,
  NULL,
  CONCAT('load-', n),
  DATE_SUB(CURDATE(), INTERVAL (n % 730) DAY),
  ((n * 7919) % 200000) + 1000,
  IF(n % 11 = 0, 'IN', 'OUT'),
  'MYDATA',
  ELT((n % 5) + 1, '스타벅스', '이마트', 'GS25', '배달의민족', '카카오T'),
  ELT((n % 5) + 1, 'CAFE', 'GROCERY', 'CONVENIENCE', 'FOOD', 'TRANSPORT'),
  NOW(6)
FROM seq;

SELECT CONCAT('시딩 완료: user_id=', @uid, ', 거래 ',
              (SELECT COUNT(*) FROM tbl_transaction WHERE user_id = @uid), '건') AS result;
