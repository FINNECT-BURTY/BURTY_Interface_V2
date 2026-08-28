/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 애플리케이션 서비스 (CashflowManagementService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.cashflow
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
package com.burty.application.service.cashflow;

import com.burty.application.dto.cashflow.CashflowCalendarDayResponse;
import com.burty.application.dto.cashflow.CashflowScheduleRequest;
import com.burty.application.dto.cashflow.CashflowScheduleResponse;
import com.burty.application.dto.cashflow.RiskCauseResponse;
import com.burty.application.port.in.cashflow.CashflowForecastUseCase;
import com.burty.application.port.in.cashflow.CashflowManagementUseCase;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.cashflow.entity.CashflowScheduleEntity;
import com.burty.domain.cashflow.entity.RecurringExpenseEntity;
import com.burty.domain.cashflow.model.CashflowForecast;
import com.burty.domain.cashflow.repository.CashflowScheduleRepository;
import com.burty.domain.cashflow.repository.RecurringExpenseRepository;
import com.burty.util.CashflowScheduleDateUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashflowManagementService implements CashflowManagementUseCase {

  private final CashflowScheduleRepository scheduleRepository;
  private final RecurringExpenseRepository recurringExpenseRepository;
  private final CashflowForecastUseCase cashflowForecastUseCase;
  private final CashflowScheduleDateUtils scheduleDateUtils;

  @Override
  public List<CashflowCalendarDayResponse> calendar(String userId) {
    CashflowForecast forecast = cashflowForecastUseCase.forecast(userId);
    Long numericUserId = scheduleDateUtils.parseNumericUserId(userId);
    Map<LocalDate, List<String>> eventMap =
        numericUserId == null ? Map.of() : eventMap(numericUserId, forecast.generatedDate());
    return forecast.dailyBalances().stream()
        .map(
            point ->
                new CashflowCalendarDayResponse(
                    point.date(),
                    point.balance(),
                    forecast.riskDate() != null && forecast.riskDate().equals(point.date()),
                    eventMap.getOrDefault(point.date(), List.of())))
        .toList();
  }

  @Override
  public List<CashflowScheduleResponse> schedules(String userId) {
    return scheduleRepository.findByUserIdAndActiveTrue(Long.parseLong(userId)).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  public CashflowScheduleResponse upsertSchedule(String userId, CashflowScheduleRequest request) {
    CashflowScheduleEntity entity = new CashflowScheduleEntity();
    // 본문이 아니라 토큰의 사용자로 저장한다. 예전에는 남의 userId 를 보내
    // 상대의 고정 지출 일정을 만들거나 바꿀 수 있었고, 그것이 곧 상대의 예측을 바꿨다.
    entity.setUserId(Long.parseLong(userId));
    entity.setScheduleTypeCode(defaultString(request.scheduleTypeCode(), "CUSTOM"));
    entity.setLabel(defaultString(request.label(), "직접 입력 일정"));
    entity.setAmount(Math.max(0L, request.amount() == null ? 0L : request.amount()));
    entity.setDirection(defaultString(request.direction(), "EXPENSE").toUpperCase());
    entity.setDayOfMonth(
        Math.max(1, Math.min(28, request.dayOfMonth() == null ? 1 : request.dayOfMonth())));
    entity.setSource("USER");
    entity.setActive(true);
    return toResponse(scheduleRepository.save(entity));
  }

  @Override
  public void deactivateSchedule(String scheduleId, String userId) {
    CashflowScheduleEntity entity =
        scheduleRepository
            .findById(Long.parseLong(scheduleId))
            .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "일정을 찾을 수 없습니다."));
    if (!String.valueOf(entity.getUserId()).equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "일정 소유자가 일치하지 않습니다.");
    }
    entity.setActive(false);
    scheduleRepository.save(entity);
  }

  @Override
  public List<RiskCauseResponse> riskCauses(String userId) {
    Long numericUserId = scheduleDateUtils.parseNumericUserId(userId);
    if (numericUserId == null) {
      CashflowForecast forecast = cashflowForecastUseCase.forecast(userId);
      return List.of(
          new RiskCauseResponse(
              "CARD_BILL",
              "카드값",
              Math.max(0L, forecast.safetyBalance() - forecast.minimumBalance()),
              "카드 결제일 생활비 지출이 안전잔액을 깎습니다."),
          new RiskCauseResponse("VARIABLE_SPEND", "예상 변동지출", 0L, "식비·카페·배달 등 바로 조정 가능한 지출입니다."),
          new RiskCauseResponse("FIXED_EXPENSE", "월세·대출·공과금", 0L, "고정지출일과 급여일 충돌 여부를 확인해야 합니다."));
    }
    List<RiskCauseResponse> causes = new ArrayList<>();
    for (CashflowScheduleEntity s : scheduleRepository.findByUserIdAndActiveTrue(numericUserId)) {
      if (!"EXPENSE".equalsIgnoreCase(s.getDirection())) continue;
      String type = normalizeCauseType(s.getScheduleTypeCode(), s.getLabel());
      causes.add(
          new RiskCauseResponse(
              type, s.getLabel(), s.getAmount(), s.getLabel() + " 일정이 예상 잔액을 낮춥니다."));
    }
    for (RecurringExpenseEntity r :
        recurringExpenseRepository.findByUserIdAndActiveTrue(numericUserId)) {
      String type = normalizeCauseType(r.getExpenseCategoryCode(), r.getName());
      causes.add(new RiskCauseResponse(type, r.getName(), r.getAvgAmount(), "반복 지출 패턴이 감지되었습니다."));
    }
    causes.sort(Comparator.comparingLong(RiskCauseResponse::impactAmount).reversed());
    if (causes.stream().noneMatch(c -> "VARIABLE_SPEND".equals(c.causeType()))) {
      causes.add(
          new RiskCauseResponse("VARIABLE_SPEND", "예상 변동지출", 0L, "식비·카페·배달 등 직접 조정 가능한 지출입니다."));
    }
    return causes.stream().limit(8).toList();
  }

  private Map<LocalDate, List<String>> eventMap(Long userId, LocalDate startDate) {
    Map<LocalDate, List<String>> map = new HashMap<>();
    for (CashflowScheduleEntity s : scheduleRepository.findByUserIdAndActiveTrue(userId)) {
      addEvent(
          map,
          scheduleDateUtils.nextOccurrence(startDate, s.getDayOfMonth()),
          s.getLabel() + " " + signedAmount(s.getDirection(), s.getAmount()));
    }
    for (RecurringExpenseEntity r : recurringExpenseRepository.findByUserIdAndActiveTrue(userId)) {
      addEvent(
          map,
          scheduleDateUtils.nextOccurrence(startDate, r.getDayOfMonth()),
          r.getName() + " -" + r.getAvgAmount() + "원");
    }
    return map;
  }

  private void addEvent(Map<LocalDate, List<String>> map, LocalDate date, String event) {
    map.computeIfAbsent(date, ignored -> new ArrayList<>()).add(event);
  }

  private String signedAmount(String direction, long amount) {
    return ("INCOME".equalsIgnoreCase(direction) ? "+" : "-") + amount + "원";
  }

  private CashflowScheduleResponse toResponse(CashflowScheduleEntity entity) {
    return new CashflowScheduleResponse(
        String.valueOf(entity.getScheduleId()),
        entity.getScheduleTypeCode(),
        entity.getLabel(),
        entity.getAmount(),
        entity.getDirection(),
        entity.getDayOfMonth(),
        Boolean.TRUE.equals(entity.getActive()));
  }

  private String normalizeCauseType(String code, String label) {
    String text = (code + " " + label).toUpperCase();
    if (text.contains("RENT") || text.contains("월세")) return "RENT";
    if (text.contains("CARD") || text.contains("카드")) return "CARD_BILL";
    if (text.contains("LOAN") || text.contains("대출")) return "LOAN";
    if (text.contains("UTIL") || text.contains("관리비") || text.contains("공과")) return "UTILITY";
    if (text.contains("SUBSCRIPTION") || text.contains("구독")) return "SUBSCRIPTION";
    return "FIXED_EXPENSE";
  }

  private String defaultString(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
