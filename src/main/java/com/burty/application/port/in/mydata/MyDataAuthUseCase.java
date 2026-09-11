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

import com.burty.application.dto.mydata.MyDataLinkResult;
import com.burty.domain.mydata.entity.MyDataLinkStatusEntity;
import java.util.List;

public interface MyDataAuthUseCase {
  String createAuthorizeUrl(String userId);

  String createAuthorizeUrl(String userId, String institutionCode);

  /** 인가를 마치면 {@code frontendOrigin} 으로 돌려보낸다. 허용 목록으로 검증한 값만 넘긴다. */
  String createAuthorizeUrl(String userId, String institutionCode, String frontendOrigin);

  /**
   * 정보제공자가 브라우저를 보내 오는 콜백을 처리한다. 예외를 던지지 않고 결과로 돌려준다 — 브라우저는 JSON 오류 화면이 아니라 FE 로 가야 한다.
   *
   * @param error 사용자가 거절하면 정보제공자는 {@code code} 없이 이것만 준다
   */
  MyDataLinkResult completeAuthorization(String state, String code, String error);

  boolean exchangeAuthorizationCode(String userId, String code);

  boolean exchangeAuthorizationCode(String userId, String institutionCode, String code);

  /** OAuth redirect 콜백: state로 사용자·기관을 복원한 뒤 토큰 교환. */
  boolean exchangeAuthorizationCodeByState(String state, String code);

  List<MyDataLinkStatusEntity> listInstitutions(String userId);

  boolean unlinkInstitution(String userId, String institutionCode);
}
