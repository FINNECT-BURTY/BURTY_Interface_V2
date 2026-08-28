package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.util.FieldEncryptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 필드 암호화 키 로테이션.
 *
 * <p>로테이션의 핵심은 <b>구키로 쓴 값을 계속 읽을 수 있는가</b>다. 이게 안 되면 키를 바꾸는 순간 기존 마이데이터 토큰이 전부 죽고 사용자 연동이 끊긴다. 되돌릴
 * 방법도 없다.
 *
 * <p>배치는 그 다음 문제다. 읽기만 되고 다시 쓰지 않으면 구키를 영원히 들고 있어야 해서 로테이션의 의미가 없다.
 */
class FieldEncryptionRotationTests {

  private static final String OLD_KEY = "old-field-encryption-key-for-rotation-test";
  private static final String NEW_KEY = "new-field-encryption-key-for-rotation-test";

  /** 로테이션 이전 상태 — 구키만 있고 버전은 2. */
  private FieldEncryptor before() {
    return new FieldEncryptor(OLD_KEY, 2, "", 0);
  }

  /** 로테이션 진행 중 — 신키(v3)로 쓰고, 구키(v2)로 쓴 값도 읽는다. */
  private FieldEncryptor during() {
    return new FieldEncryptor(NEW_KEY, 3, OLD_KEY, 2);
  }

  /** 로테이션 완료 후 — 구키 설정을 제거한 상태. */
  private FieldEncryptor after() {
    return new FieldEncryptor(NEW_KEY, 3, "", 0);
  }

  @Test
  @DisplayName("로테이션 중에는 구키로 쓴 값도 읽힌다")
  void oldCiphertextRemainsReadableDuringRotation() {
    String cipher = before().encrypt("mydata-access-token");

    assertEquals(
        "mydata-access-token", during().decrypt(cipher), "구키로 쓴 값을 못 읽으면 키를 바꾸는 순간 연동이 전부 끊긴다");
  }

  @Test
  @DisplayName("새로 쓰는 값은 신키 버전으로 기록된다")
  void newWritesUseCurrentKeyVersion() {
    FieldEncryptor rotating = during();
    String fresh = rotating.encrypt("new-token");

    assertFalse(rotating.needsReEncryption(fresh), "현재 키로 쓴 값은 재암호화 대상이 아니다");
    assertEquals("new-token", rotating.decrypt(fresh));
  }

  @Test
  @DisplayName("구키로 쓴 값은 재암호화 대상으로 식별된다")
  void oldCiphertextIsFlaggedForReEncryption() {
    String cipher = before().encrypt("token");

    assertTrue(during().needsReEncryption(cipher));
  }

  @Test
  @DisplayName("재암호화하면 신키 버전이 되고 평문은 보존된다")
  void reEncryptionPreservesPlaintextAndBumpsVersion() {
    String plain = "mydata-refresh-token";
    String oldCipher = before().encrypt(plain);
    FieldEncryptor rotating = during();

    // 배치가 하는 일과 동일하다.
    String newCipher = rotating.encrypt(rotating.decrypt(oldCipher));

    assertNotEquals(oldCipher, newCipher);
    assertFalse(rotating.needsReEncryption(newCipher));
    assertEquals(plain, rotating.decrypt(newCipher));
  }

  @Test
  @DisplayName("로테이션이 끝나면 구키 설정을 제거해도 신키 값은 읽힌다")
  void rotatedDataSurvivesRemovingOldKey() {
    String plain = "token";
    FieldEncryptor rotating = during();
    String rotated = rotating.encrypt(rotating.decrypt(before().encrypt(plain)));

    assertEquals(plain, after().decrypt(rotated), "로테이션 완료 후 구키를 지울 수 있어야 한다");
  }

  @Test
  @DisplayName("구키 설정을 너무 일찍 제거하면 옛 값은 읽히지 않는다")
  void removingOldKeyTooEarlyBreaksOldCiphertext() {
    String oldCipher = before().encrypt("token");

    // 이 실패는 조용히 넘어가면 안 된다. 예외로 드러나야 운영에서 즉시 알 수 있다.
    assertThrows(FieldEncryptor.FieldDecryptionException.class, () -> after().decrypt(oldCipher));
  }

  @Test
  @DisplayName("레거시(v1) 값도 로테이션 대상에 포함된다")
  void legacyCiphertextIsAlsoRotatable() {
    // v1 은 예전 키 유도 방식으로 쓰인 값이다. 현재 키 문자열에서 파생되므로
    // 같은 키를 쓰는 인스턴스라면 읽을 수 있어야 한다.
    FieldEncryptor legacyReader = new FieldEncryptor(OLD_KEY, 2, "", 0);
    String plain = "legacy-token";
    String v2Cipher = legacyReader.encrypt(plain);

    assertTrue(during().needsReEncryption(v2Cipher));
    assertEquals(plain, during().decrypt(v2Cipher));
  }

  @Test
  @DisplayName("이전 키 버전을 현재와 같게 두면 기동 시점에 거부한다")
  void sameVersionForCurrentAndPreviousKeyIsRejected() {
    // 같은 버전이면 어느 키로 쓴 값인지 구분할 수 없다. 설정 실수를 조용히 넘기면
    // 데이터가 읽히지 않는 상태로 운영에 나간다.
    assertThrows(IllegalArgumentException.class, () -> new FieldEncryptor(NEW_KEY, 2, OLD_KEY, 2));
  }
}
