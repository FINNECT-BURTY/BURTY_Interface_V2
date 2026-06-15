/**
 *
 *
 * <pre>
 * <b>Description  : 마이데이터 유스케이스 포트 (MyDataAuthUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.mydata
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
package com.burty.application.port.in.mydata;

import com.burty.domain.mydata.entity.MyDataLinkStatusEntity;
import java.util.List;

public interface MyDataAuthUseCase {
  String createAuthorizeUrl(String userId);

  String createAuthorizeUrl(String userId, String institutionCode);

  boolean exchangeAuthorizationCode(String userId, String code);

  boolean exchangeAuthorizationCode(String userId, String institutionCode, String code);

  /** OAuth redirect 콜백: state로 사용자·기관을 복원한 뒤 토큰 교환. */
  boolean exchangeAuthorizationCodeByState(String state, String code);

  List<MyDataLinkStatusEntity> listInstitutions(String userId);

  boolean unlinkInstitution(String userId, String institutionCode);
}
