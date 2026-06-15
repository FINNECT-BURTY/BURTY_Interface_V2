/**
 *
 *
 * <pre>
 * <b>Description  : 자산 애플리케이션 서비스 (AssetSummaryService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.asset
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.application.service.asset;

import com.burty.application.dto.asset.AssetSummaryResponse;
import com.burty.application.dto.asset.AssetTrendItemResponse;
import com.burty.application.port.in.asset.AssetSummaryUseCase;
import com.burty.application.port.out.mydata.MyDataPort;
import com.burty.domain.asset.model.AssetSnapshot;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetSummaryService implements AssetSummaryUseCase {

  private final MyDataPort myDataPort;

  @Override
  public AssetSummaryResponse summary(String userId) {
    AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
    return new AssetSummaryResponse(
        userId, snapshot.totalAsset(), snapshot.monthlySpend(), snapshot.volatilityPercent());
  }

  @Override
  public List<AssetTrendItemResponse> trend(String userId) {
    AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
    List<AssetTrendItemResponse> trend = new ArrayList<>();
    YearMonth now = YearMonth.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
    for (int i = 5; i >= 0; i--) {
      YearMonth point = now.minusMonths(i);
      trend.add(
          new AssetTrendItemResponse(
              point.format(formatter), Math.round(snapshot.totalAsset() * (1 - (i * 0.01)))));
    }
    return trend;
  }
}
