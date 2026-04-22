-- ============================================================================
-- USE CASE 01. 회원가입 & 본인인증
-- ----------------------------------------------------------------------------
-- 시나리오: 사용자가 앱을 처음 설치하고 휴대폰 본인인증을 거쳐 계정을 생성
-- 빈도: 일 100~1000건 (초기), 일 1만건 (정식 런칭 후)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1.1 기존 가입자 확인 (회원가입 전)
-- ----------------------------------------------------------------------------
-- CI 해시로 이미 가입된 사용자인지 확인
-- 성능 목표: < 10ms (uk_user_ci_hash 활용)
SELECT
    BIN_TO_UUID(user_id, 1)  AS user_id,
    status,
    created_at
FROM tbl_user
WHERE ci_hash = ?  -- SHA-256(CI)
  AND status <> 'WITHDRAWN';


-- ----------------------------------------------------------------------------
-- 1.2 신규 사용자 가입 (트랜잭션)
-- ----------------------------------------------------------------------------
-- tbl_user, tbl_user_profile, tbl_consent_record 동시 생성
START TRANSACTION;

SET @user_id = UUID_TO_BIN(UUID(), 1);

INSERT INTO tbl_user (
    user_id, ci_hash, ci_encrypted,
    phone_hash, phone_encrypted, status
) VALUES (
    @user_id,
    ?,  -- SHA-256(CI)
    ?,  -- AES-256-GCM(CI)
    ?,  -- SHA-256(phone)
    ?,  -- AES-256-GCM(phone)
    'ACTIVE'
);

INSERT INTO tbl_user_profile (
    user_id, name_encrypted, birthdate_encrypted,
    age_range, ux_mode, font_scale, voice_enabled
) VALUES (
    @user_id,
    ?,  -- AES-256-GCM(name)
    ?,  -- AES-256-GCM(YYYYMMDD)
    ?,  -- 연령대 계산 결과 (55s=55, 60s=60, 65s=65)
    CASE WHEN ? >= 55 THEN 'SENIOR' ELSE 'STANDARD' END,  -- 55세 이상 시니어 모드
    CASE WHEN ? >= 55 THEN 1.30 ELSE 1.00 END,
    CASE WHEN ? >= 55 THEN TRUE ELSE FALSE END
);

-- 필수 동의 3개를 한 번에 기록
INSERT INTO tbl_consent_record (
    consent_id, user_id, consent_type, consent_version,
    document_hash, agreed_at, ip_address, user_agent
) VALUES
    (UUID_TO_BIN(UUID(), 1), @user_id, 'TERMS',   'v2.1', ?, NOW(6), ?, ?),
    (UUID_TO_BIN(UUID(), 1), @user_id, 'PRIVACY', 'v2.1', ?, NOW(6), ?, ?),
    (UUID_TO_BIN(UUID(), 1), @user_id, 'MYDATA',  'v1.3', ?, NOW(6), ?, ?);

COMMIT;


-- ----------------------------------------------------------------------------
-- 1.3 디바이스 등록 (로그인 시)
-- ----------------------------------------------------------------------------
-- 같은 디바이스 재로그인 시 UPSERT
INSERT INTO tbl_device (
    device_id, user_id, device_fingerprint, platform,
    os_version, app_version, fcm_token, last_seen_at
) VALUES (
    UUID_TO_BIN(UUID(), 1),
    ?,  -- user_id
    ?,  -- device fingerprint (SHA-256)
    ?,  -- 'IOS' | 'ANDROID' | 'WEB'
    ?, ?, ?,
    NOW(6)
)
ON DUPLICATE KEY UPDATE
    os_version = VALUES(os_version),
    app_version = VALUES(app_version),
    fcm_token = VALUES(fcm_token),
    last_seen_at = NOW(6),
    revoked_at = NULL;  -- 재활성화


-- ----------------------------------------------------------------------------
-- 1.4 FIDO2 생체인증 등록
-- ----------------------------------------------------------------------------
-- WebAuthn 공개키 등록 (지문/Face ID)
INSERT INTO tbl_biometric_credential (
    credential_id, user_id, device_id, credential_type,
    public_key, credential_id_raw, aaguid, registered_at
) VALUES (
    UUID_TO_BIN(UUID(), 1),
    ?,  -- user_id
    ?,  -- device_id
    ?,  -- 'FINGERPRINT' | 'FACE_ID'
    ?,  -- 공개키 바이너리
    ?,  -- WebAuthn credential ID
    ?,  -- 인증기 AAGUID
    NOW(6)
);


-- ----------------------------------------------------------------------------
-- 1.5 로그인 (디바이스 검증 + 실패 카운트)
-- ----------------------------------------------------------------------------
-- 로그인 실패 시 카운트 증가 (5회 초과 시 앱 레벨에서 차단)
UPDATE tbl_user
SET failed_login_count = failed_login_count + 1,
    updated_at = NOW(6)
WHERE user_id = ?;

-- 로그인 성공 시 카운트 초기화 + 최종 로그인 갱신
UPDATE tbl_user
SET failed_login_count = 0,
    last_login_at = NOW(6),
    last_login_ip = INET6_ATON(?),  -- IP 문자열 -> binary
    updated_at = NOW(6)
WHERE user_id = ?;


-- ----------------------------------------------------------------------------
-- 1.6 유효한 동의 조회 (마이데이터 연동 전 체크)
-- ----------------------------------------------------------------------------
-- 성능 목표: < 5ms (idx_consent_user_type 커버)
SELECT
    consent_type,
    consent_version,
    agreed_at
FROM tbl_consent_record
WHERE user_id = ?
  AND consent_type IN ('TERMS', 'PRIVACY', 'MYDATA')
  AND revoked_at IS NULL
ORDER BY agreed_at DESC;


-- ----------------------------------------------------------------------------
-- 1.7 동의 철회 (append-only, UPDATE로 revoked_at 기록)
-- ----------------------------------------------------------------------------
UPDATE tbl_consent_record
SET revoked_at = NOW(6),
    revoke_reason = ?
WHERE consent_id = ?
  AND revoked_at IS NULL;


-- ----------------------------------------------------------------------------
-- 1.8 회원 탈퇴 (soft delete, 금융 데이터는 법정 보관 기간 유지)
-- ----------------------------------------------------------------------------
-- 하드 삭제 절대 금지. status만 변경.
START TRANSACTION;

UPDATE tbl_user
SET status = 'WITHDRAWN',
    withdrawn_at = NOW(6),
    updated_at = NOW(6)
WHERE user_id = ?;

-- 모든 디바이스 revoke
UPDATE tbl_device
SET revoked_at = NOW(6)
WHERE user_id = ?
  AND revoked_at IS NULL;

-- 모든 생체 자격증명 revoke
UPDATE tbl_biometric_credential
SET revoked_at = NOW(6)
WHERE user_id = ?
  AND revoked_at IS NULL;

-- 마이데이터 연결 전부 REVOKED
UPDATE tbl_linked_institution
SET status = 'REVOKED',
    updated_at = NOW(6)
WHERE user_id = ?
  AND status = 'ACTIVE';

COMMIT;
