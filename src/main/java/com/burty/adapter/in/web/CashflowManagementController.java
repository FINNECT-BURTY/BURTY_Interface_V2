package com.burty.adapter.in.web;

import com.burty.core.controller.BaseController;

import com.burty.adapter.in.web.dto.*;
import com.burty.application.port.in.CashflowForecastUseCase;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.entity.CashflowScheduleEntity;
import com.burty.domain.entity.RecurringExpenseEntity;
import com.burty.domain.model.CashflowForecast;
import com.burty.domain.repository.CashflowScheduleRepository;
import com.burty.domain.repository.RecurringExpenseRepository;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/cashflow-management")
@Tag(name = "BURTY Cashflow Management", description = "현금흐름 캘린더/고정지출/위험원인 관리 API")
public class CashflowManagementController extends BaseController {
    private final CashflowScheduleRepository scheduleRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final CashflowForecastUseCase cashflowForecastUseCase;

    public CashflowManagementController(CashflowScheduleRepository scheduleRepository,
                                        RecurringExpenseRepository recurringExpenseRepository,
                                        CashflowForecastUseCase cashflowForecastUseCase) {
        this.scheduleRepository = scheduleRepository;
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.cashflowForecastUseCase = cashflowForecastUseCase;
    }

    @GetMapping("/calendar")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "현금흐름 캘린더", description = "30일 예상 잔액과 월세/카드/대출/급여 이벤트를 일자별로 반환합니다.")
    public ApiResponse<List<CashflowCalendarDayResponse>> calendar(@RequestParam String userId) {
        CashflowForecast forecast = cashflowForecastUseCase.forecast(userId);
        Long numericUserId = parseUserId(userId);
        Map<LocalDate, List<String>> eventMap = numericUserId == null ? Map.of() : eventMap(numericUserId, forecast.getGeneratedDate());
        List<CashflowCalendarDayResponse> days = forecast.getDailyBalances().stream()
                .map(point -> new CashflowCalendarDayResponse(
                        point.getDate(),
                        point.getBalance(),
                        forecast.getRiskDate() != null && forecast.getRiskDate().equals(point.getDate()),
                        eventMap.getOrDefault(point.getDate(), List.of())
                ))
                .toList();
        return ApiResponse.ok(days);
    }

    @GetMapping("/schedules")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<List<CashflowScheduleResponse>> schedules(@RequestParam String userId) {
        return ApiResponse.ok(scheduleRepository.findByUserIdAndActiveTrue(Long.parseLong(userId)).stream()
                .map(this::toResponse)
                .toList());
    }

    @PostMapping("/schedules")
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "고정 수입/지출 등록", description = "월세, 관리비, 통신비, 구독료, 대출 상환일 등을 직접 등록합니다.")
    public ApiResponse<CashflowScheduleResponse> upsertSchedule(@RequestBody CashflowScheduleRequest request) {
        CashflowScheduleEntity entity = new CashflowScheduleEntity();
        entity.setUserId(Long.parseLong(request.getUserId()));
        entity.setScheduleTypeCode(defaultString(request.getScheduleTypeCode(), "CUSTOM"));
        entity.setLabel(defaultString(request.getLabel(), "직접 입력 일정"));
        entity.setAmount(Math.max(0L, request.getAmount() == null ? 0L : request.getAmount()));
        entity.setDirection(defaultString(request.getDirection(), "EXPENSE").toUpperCase());
        entity.setDayOfMonth(Math.max(1, Math.min(28, request.getDayOfMonth() == null ? 1 : request.getDayOfMonth())));
        entity.setSource("USER");
        entity.setActive(true);
        return ApiResponse.ok(toResponse(scheduleRepository.save(entity)));
    }

    @DeleteMapping("/schedules/{scheduleId}")
    @AuthLevel(RiskLevel.LEVEL_2)
    public ApiResponse<SimpleResultResponse> deactivateSchedule(@PathVariable String scheduleId, @RequestParam String userId) {
        CashflowScheduleEntity entity = scheduleRepository.findById(Long.parseLong(scheduleId)).orElseThrow();
        if (!String.valueOf(entity.getUserId()).equals(userId)) throw new IllegalArgumentException("owner mismatch");
        entity.setActive(false);
        scheduleRepository.save(entity);
        return ApiResponse.ok(new SimpleResultResponse(true, "현금흐름 일정이 비활성화되었습니다."));
    }

    @GetMapping("/risk-causes")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "위험 원인 분해", description = "월세/카드/대출/변동지출 중 어떤 요인이 잔액 위험을 키우는지 반환합니다.")
    public ApiResponse<List<RiskCauseResponse>> riskCauses(@RequestParam String userId) {
        Long numericUserId = parseUserId(userId);
        if (numericUserId == null) {
            CashflowForecast forecast = cashflowForecastUseCase.forecast(userId);
            return ApiResponse.ok(List.of(
                    new RiskCauseResponse("CARD_BILL", "카드값", Math.max(0L, forecast.getSafetyBalance() - forecast.getMinimumBalance()),
                            "카드 결제와 생활비 지출이 안전잔액을 낮춥니다."),
                    new RiskCauseResponse("VARIABLE_SPEND", "일상 변동지출", 0L,
                            "식비·카페·배달 등 바로 조정 가능한 지출입니다."),
                    new RiskCauseResponse("FIXED_EXPENSE", "월세·대출·공과금", 0L,
                            "고정지출일과 급여일 충돌 여부를 확인해야 합니다.")
            ));
        }
        List<RiskCauseResponse> causes = new ArrayList<>();
        for (CashflowScheduleEntity s : scheduleRepository.findByUserIdAndActiveTrue(numericUserId)) {
            if (!"EXPENSE".equalsIgnoreCase(s.getDirection())) continue;
            String type = normalizeCauseType(s.getScheduleTypeCode(), s.getLabel());
            causes.add(new RiskCauseResponse(type, s.getLabel(), s.getAmount(), type + " 일정이 예상 잔액을 낮춥니다."));
        }
        for (RecurringExpenseEntity r : recurringExpenseRepository.findByUserIdAndActiveTrue(numericUserId)) {
            String type = normalizeCauseType(r.getExpenseCategoryCode(), r.getName());
            causes.add(new RiskCauseResponse(type, r.getName(), r.getAvgAmount(), "반복 지출 패턴이 감지되었습니다."));
        }
        causes.sort(Comparator.comparingLong(RiskCauseResponse::getImpactAmount).reversed());
        if (causes.stream().noneMatch(c -> "VARIABLE_SPEND".equals(c.getCauseType()))) {
            causes.add(new RiskCauseResponse("VARIABLE_SPEND", "일상 변동지출", 0L, "식비·카페·배달 등 직접 조정 가능한 지출입니다."));
        }
        return ApiResponse.ok(causes.stream().limit(8).toList());
    }

    private Map<LocalDate, List<String>> eventMap(Long userId, LocalDate startDate) {
        Map<LocalDate, List<String>> map = new HashMap<>();
        for (CashflowScheduleEntity s : scheduleRepository.findByUserIdAndActiveTrue(userId)) {
            addEvent(map, nextOccurrence(startDate, s.getDayOfMonth()), s.getLabel() + " " + signedAmount(s.getDirection(), s.getAmount()));
        }
        for (RecurringExpenseEntity r : recurringExpenseRepository.findByUserIdAndActiveTrue(userId)) {
            addEvent(map, nextOccurrence(startDate, r.getDayOfMonth()), r.getName() + " -" + r.getAvgAmount() + "원");
        }
        return map;
    }

    private void addEvent(Map<LocalDate, List<String>> map, LocalDate date, String event) {
        map.computeIfAbsent(date, ignored -> new ArrayList<>()).add(event);
    }

    private LocalDate nextOccurrence(LocalDate startDate, int dayOfMonth) {
        int day = Math.max(1, Math.min(28, dayOfMonth));
        LocalDate candidate = startDate.withDayOfMonth(Math.min(day, startDate.lengthOfMonth()));
        if (candidate.isBefore(startDate)) {
            candidate = candidate.plusMonths(1).withDayOfMonth(Math.min(day, candidate.plusMonths(1).lengthOfMonth()));
        }
        return candidate;
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
                Boolean.TRUE.equals(entity.getActive())
        );
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

    private Long parseUserId(String userId) {
        if (userId == null || userId.isBlank()) return null;
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
