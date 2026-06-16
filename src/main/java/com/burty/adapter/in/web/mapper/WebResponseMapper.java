/**
 *
 *
 * <pre>
 * <b>Description  : 웹 API 응답 매퍼 (WebResponseMapper)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.mapper
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
package com.burty.adapter.in.web.mapper;

import com.burty.application.dto.action.ActionExecutionResponse;
import com.burty.application.dto.action.ActionFeedbackSummaryResponse;
import com.burty.application.dto.action.ActionRecommendationResponse;
import com.burty.application.dto.admin.AuditLogResponse;
import com.burty.application.dto.auth.BiometricAuthResponse;
import com.burty.application.dto.cashflow.CashflowCriteriaResponse;
import com.burty.application.dto.cashflow.CashflowForecastResponse;
import com.burty.application.dto.cashflow.RecurringExpenseResponse;
import com.burty.application.dto.cashflow.RiskAssessmentResponse;
import com.burty.application.dto.consult.ConsultResponse;
import com.burty.application.dto.consult.MonthlyReportResponse;
import com.burty.application.dto.family.FamilyAlertResponse;
import com.burty.application.dto.family.FamilyConsentResponse;
import com.burty.application.dto.family.FamilyDashboardResponse;
import com.burty.application.dto.finance.TransferResponse;
import com.burty.application.dto.policy.PolicyAdminResponse;
import com.burty.application.dto.policy.PolicyMatchResponse;
import com.burty.application.dto.user.DeviceResponse;
import com.burty.application.dto.user.PersonaResponse;
import com.burty.core.config.GlobalMapperConfig;
import com.burty.domain.action.model.ActionExecutionResult;
import com.burty.domain.action.model.ActionFeedbackSummary;
import com.burty.domain.action.model.ActionRecommendation;
import com.burty.domain.admin.entity.AuditLogEntity;
import com.burty.domain.auth.model.BiometricAuthResult;
import com.burty.domain.cashflow.model.CashflowCriteria;
import com.burty.domain.cashflow.model.CashflowForecast;
import com.burty.domain.cashflow.model.RecurringExpense;
import com.burty.domain.cashflow.model.RiskAssessment;
import com.burty.domain.consult.model.ConsultationResult;
import com.burty.domain.consult.model.MonthlyReport;
import com.burty.domain.family.model.FamilyAlert;
import com.burty.domain.family.model.FamilyConsent;
import com.burty.domain.family.model.FamilyDashboardSummary;
import com.burty.domain.finance.model.TransferResult;
import com.burty.domain.policy.entity.PolicyEntity;
import com.burty.domain.policy.model.PolicyMatch;
import com.burty.domain.user.entity.DeviceEntity;
import com.burty.domain.user.entity.PersonaProfileEntity;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", config = GlobalMapperConfig.class)
public interface WebResponseMapper {

  ConsultResponse toResponse(ConsultationResult result);

  MonthlyReportResponse toResponse(MonthlyReport report);

  TransferResponse toResponse(TransferResult result);

  List<TransferResponse> toTransferResponses(List<TransferResult> results);

  FamilyAlertResponse toResponse(FamilyAlert alert);

  List<FamilyAlertResponse> toFamilyAlertResponses(List<FamilyAlert> alerts);

  FamilyConsentResponse toResponse(FamilyConsent consent);

  List<FamilyConsentResponse> toFamilyConsentResponses(List<FamilyConsent> consents);

  FamilyDashboardResponse toResponse(FamilyDashboardSummary summary);

  CashflowForecastResponse toResponse(CashflowForecast forecast);

  CashflowCriteriaResponse toResponse(CashflowCriteria criteria);

  RiskAssessmentResponse toResponse(RiskAssessment risk);

  ActionRecommendationResponse toResponse(ActionRecommendation action);

  PolicyMatchResponse toResponse(PolicyMatch match);

  List<PolicyMatchResponse> toPolicyMatchResponses(List<PolicyMatch> matches);

  RecurringExpenseResponse toResponse(RecurringExpense expense);

  List<RecurringExpenseResponse> toRecurringExpenseResponses(List<RecurringExpense> expenses);

  ActionExecutionResponse toResponse(ActionExecutionResult result);

  ActionFeedbackSummaryResponse toResponse(ActionFeedbackSummary summary);

  default PersonaResponse toResponse(PersonaProfileEntity entity) {
    return PersonaResponse.from(entity);
  }

  default DeviceResponse toResponse(DeviceEntity device) {
    return new DeviceResponse(
        device.getDeviceId().toString(),
        device.getDeviceName(),
        device.getPlatform() == null ? null : device.getPlatform().name(),
        device.getOsVersion(),
        device.getAppVersion(),
        Boolean.TRUE.equals(device.getIsTrusted()),
        device.getLastSeenAt(),
        device.getCreatedAt());
  }

  default PolicyAdminResponse toResponse(PolicyEntity entity) {
    return new PolicyAdminResponse(
        entity.getPolicyCode(),
        entity.getPolicyTypeCode(),
        entity.getTitle(),
        entity.getApplyUrl(),
        entity.getValidTo(),
        Boolean.TRUE.equals(entity.getActive()));
  }

  default AuditLogResponse toResponse(AuditLogEntity entity) {
    return new AuditLogResponse(
        entity.getAuditId(),
        entity.getOccurredAt(),
        entity.getActorType() == null ? null : entity.getActorType().name(),
        entity.getAction(),
        entity.getResult() == null ? null : entity.getResult().name(),
        entity.getTargetType(),
        entity.getMetadata());
  }

  default BiometricAuthResponse toBiometricResponse(
      BiometricAuthResult result, String riskProof, boolean includeDeviceToken) {
    return new BiometricAuthResponse(
        result.userId(),
        result.deviceId(),
        includeDeviceToken ? result.deviceToken() : null,
        result.accessToken(),
        riskProof,
        result.authenticated(),
        result.trustedDevice());
  }
}
