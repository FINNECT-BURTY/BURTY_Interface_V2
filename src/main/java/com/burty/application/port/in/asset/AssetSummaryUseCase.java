/**
 *
 *
 * <pre>
 * <b>Description  : 자산 유스케이스 포트 (AssetSummaryUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.asset
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
package com.burty.application.port.in.asset;

import com.burty.application.dto.asset.AssetSummaryResponse;
import com.burty.application.dto.asset.AssetTrendItemResponse;
import java.util.List;

public interface AssetSummaryUseCase {

  AssetSummaryResponse summary(String userId);

  List<AssetTrendItemResponse> trend(String userId);
}
