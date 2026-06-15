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

import com.burty.application.dto.user.DeviceNameUpdateRequest;
import com.burty.application.dto.user.DeviceResponse;
import java.util.List;

public interface DeviceManagementUseCase {

  List<DeviceResponse> listDevices(String userId);

  DeviceResponse updateDeviceName(String deviceId, DeviceNameUpdateRequest request);

  void revokeDevice(String deviceId, String userId);
}
