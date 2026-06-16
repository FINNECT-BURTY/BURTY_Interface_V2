/**
 *
 *
 * <pre>
 * <b>Description  : 자산 API 컨트롤러 (AssetSummaryController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.asset
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
package com.burty.adapter.in.web.asset;

import com.burty.application.dto.asset.AssetSummaryResponse;
import com.burty.application.dto.asset.AssetTrendItemResponse;
import com.burty.application.port.in.asset.AssetSummaryUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "BURTY Assets", description = "자산 요약 및 추이 API")
public class AssetSummaryController extends BaseController {

  private final AssetSummaryUseCase assetSummaryUseCase;

  @GetMapping("/assets/summary")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<AssetSummaryResponse> assetSummary(@RequestParam String userId) {
    return ApiResponse.ok(assetSummaryUseCase.summary(userId));
  }

  @GetMapping("/assets/trend")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<List<AssetTrendItemResponse>> assetTrend(@RequestParam String userId) {
    return ApiResponse.ok(assetSummaryUseCase.trend(userId));
  }
}
