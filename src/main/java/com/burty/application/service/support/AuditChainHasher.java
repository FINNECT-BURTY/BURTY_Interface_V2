package com.burty.application.service.support;

import com.burty.domain.admin.entity.AuditLogEntity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 감사 로그 해시 체인 계산. 저장 시점과 검증 시점이 <b>반드시 같은 규칙</b>을 써야 하므로 한 곳에 모아 둔다. */
public final class AuditChainHasher {

  /** 체인의 시작점. 첫 행의 prevHash. */
  public static final String GENESIS = "0".repeat(64);

  private AuditChainHasher() {}

  /**
   * 행의 내용을 정규화해 해시한다.
   *
   * <p>포함 필드는 "위조되면 곤란한" 것들로 한정한다. 표시용 필드까지 넣으면 스키마가 바뀔 때마다 과거 체인이 통째로 깨진다.
   */
  public static String hash(AuditLogEntity entity, String prevHash) {
    String canonical =
        String.join(
            "|",
            nullSafe(prevHash),
            String.valueOf(entity.getChainSeq()),
            String.valueOf(entity.getOccurredAt()),
            String.valueOf(entity.getActorType()),
            String.valueOf(entity.getActorId()),
            nullSafe(entity.getTargetType()),
            String.valueOf(entity.getTargetId()),
            nullSafe(entity.getAction()),
            String.valueOf(entity.getResult()),
            nullSafe(entity.getRequestId()),
            nullSafe(entity.getMetadata()));
    return sha256(canonical);
  }

  public static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 미지원", e);
    }
  }

  private static String nullSafe(String value) {
    return value == null ? "" : value;
  }
}
