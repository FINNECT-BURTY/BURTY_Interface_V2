package com.burty.application.port.in.asset;

import com.burty.domain.asset.model.AssetSnapshot;

/**
 * 사용자의 자산 스냅샷.
 *
 * <p>예전에는 외부 어댑터 포트({@code MyDataPort})가 곧 스냅샷이었다. 이제 스냅샷은 연결된 기관들을 도는 애플리케이션 로직이고, 외부 호출은 {@link
 * com.burty.application.port.out.mydata.MyDataBankPort} 로 갈렸다. 소비처는 이 입력 포트에 의존한다.
 */
public interface AssetSnapshotQuery {
  AssetSnapshot fetchAssetSnapshot(String userId);
}
