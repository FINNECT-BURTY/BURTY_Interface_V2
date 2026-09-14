/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 유스케이스 포트 (DeviceManagementUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.user
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
package com.burty.application.port.in.user;

import com.burty.application.dto.user.DeviceFcmTokenRequest;
import com.burty.application.dto.user.DeviceNameUpdateRequest;
import com.burty.application.dto.user.DeviceResponse;
import java.util.List;

public interface DeviceManagementUseCase {

  List<DeviceResponse> listDevices(String userId);

  /**
   * 푸시 등록 토큰을 기기에 저장한다.
   *
   * <p>기기에는 {@code fcm_token} 컬럼과 그것을 읽는 발송 경로가 이미 있었지만 값을 넣는 곳이 없었다. 그래서 푸시 대상은 항상 비어 있었고, 알림은 한
   * 건도 가지 않았다.
   *
   * @param userId 인증 토큰에서 꺼낸 사용자. 요청 본문의 값은 신뢰하지 않는다.
   */
  DeviceResponse registerFcmToken(String deviceId, String userId, DeviceFcmTokenRequest request);

  /**
   * @param userId 인증 토큰에서 꺼낸 사용자. 요청 본문의 userId 는 신뢰하지 않는다.
   */
  DeviceResponse updateDeviceName(String deviceId, String userId, DeviceNameUpdateRequest request);

  void revokeDevice(String deviceId, String userId);
}
