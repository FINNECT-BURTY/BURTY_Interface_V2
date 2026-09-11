package com.burty.domain.asset.model;

/**
 * 연결된 기관 전체를 합산한 자산 스냅샷.
 *
 * <p>연동 여부를 함께 싣는다. 예전에는 연결하지 않은 사용자에게도 고정값(총자산 3억 2천만 원)을 돌려줘서, 0 이 "자산이 없다" 인지 "모른다" 인지 가를 방법이
 * 없었다. 받는 쪽은 {@link #linked()} 로 먼저 가른다.
 *
 * @param linkedInstitutionCount 합산 대상이었던 ACTIVE 연동 기관 수
 * @param failedInstitutionCount 그중 조회에 실패해 합산에서 빠진 기관 수
 */
public record AssetSnapshot(
    double totalAsset,
    double monthlySpend,
    double volatilityPercent,
    int linkedInstitutionCount,
    int failedInstitutionCount) {

  private static final AssetSnapshot NOT_LINKED = new AssetSnapshot(0, 0, 0, 0, 0);

  public static AssetSnapshot notLinked() {
    return NOT_LINKED;
  }

  /** 합산할 연동이 하나라도 있었는지. 없으면 금액은 "0원" 이 아니라 "모름" 이다. */
  public boolean linked() {
    return linkedInstitutionCount > 0;
  }

  /** 일부 기관이 빠진 합계인지. 빠진 채로 보여줄 때는 그 사실을 함께 알려야 한다. */
  public boolean partial() {
    return failedInstitutionCount > 0;
  }
}
