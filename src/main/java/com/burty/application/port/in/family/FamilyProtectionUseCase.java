/**
 *
 *
 * <pre>
 * <b>Description  : 가족보호 유스케이스 포트 (FamilyProtectionUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.family
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
package com.burty.application.port.in.family;

import com.burty.domain.family.model.FamilyAlert;
import com.burty.domain.family.model.FamilyConsent;
import com.burty.domain.family.model.FamilyDashboardSummary;
import java.util.List;

public interface FamilyProtectionUseCase {

  List<FamilyAlert> getFamilyAlerts(String userId);

  void registerFamilyConsent(String parentUserId, String childUserId);

  boolean updateFamilyConsent(String parentUserId, String childUserId, boolean consented);

  boolean revokeFamilyConsent(String parentUserId, String childUserId);

  List<FamilyConsent> getFamilyConsents(String parentUserId);

  FamilyDashboardSummary getFamilyDashboardSummary(String userId);
}
