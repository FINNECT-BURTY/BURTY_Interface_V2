package com.burty.adapter.in.web;

import com.burty.core.controller.BaseController;

import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.entity.CashflowScheduleEntity;
import com.burty.domain.entity.PersonaProfileEntity;
import com.burty.domain.entity.RecurringExpenseEntity;
import com.burty.domain.entity.UserEntity;
import com.burty.domain.entity.UserSettingEntity;
import com.burty.domain.repository.CashflowScheduleRepository;
import com.burty.domain.repository.PersonaProfileRepository;
import com.burty.domain.repository.RecurringExpenseRepository;
import com.burty.domain.repository.UserRepository;
import com.burty.domain.repository.UserSettingRepository;
import com.burty.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/demo")
@Tag(name = "BURTY Demo", description = "MVP 시연용 사용자와 현금흐름 데이터를 생성합니다.")
public class DemoController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(DemoController.class);
    private static final String DEMO_USER_ID = "demo-user";

    private final UserRepository userRepository;
    private final CashflowScheduleRepository scheduleRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final UserSettingRepository userSettingRepository;
    private final PersonaProfileRepository personaProfileRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public DemoController(UserRepository userRepository,
                          CashflowScheduleRepository scheduleRepository,
                          RecurringExpenseRepository recurringExpenseRepository,
                          UserSettingRepository userSettingRepository,
                          PersonaProfileRepository personaProfileRepository,
                          JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.scheduleRepository = scheduleRepository;
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.userSettingRepository = userSettingRepository;
        this.personaProfileRepository = personaProfileRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/session")
    @Operation(summary = "데모 세션 생성", description = "사회초년생 1인 가구 시나리오 데이터를 만들고 JWT를 반환합니다.")
    public ApiResponse<Map<String, Object>> createDemoSession() {
        seedSettings(DEMO_USER_ID);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", DEMO_USER_ID);
        response.put("accessToken", jwtTokenProvider.generateToken(DEMO_USER_ID));
        response.put("persona", "월말 적자 반복형 사회초년생 직장인");
        response.put("scenario", "월세 납부 후 잔액 61만원, 카드값 52만원 결제 예정, 월급일까지 14일 남은 상황");
        response.put("homeUrl", "/index.html");
        return ApiResponse.ok(response);
    }

    private void tryUpsertUser(Long userId) {
        try {
            UserEntity user = userRepository.findById(userId).orElseGet(UserEntity::new);
            user.setCiHash("c".repeat(64));
            user.setCiEncrypted("demo-ci".getBytes());
            user.setPhoneHash("d".repeat(64));
            user.setPhoneEncrypted("demo-phone".getBytes());
            user.setStatus(UserEntity.UserStatus.ACTIVE);
            user.setFailedLoginCount(0);
            if (user.getCreatedAt() == null) {
                user.setCreatedAt(LocalDateTime.now());
            }
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        } catch (Exception e) {
            log.warn("Demo user upsert skipped. Existing tbl_user schema may be legacy-compatible only. err={}", e.getMessage());
        }
    }

    private void resetCashflowInputs(Long userId) {
        for (CashflowScheduleEntity schedule : scheduleRepository.findByUserIdAndActiveTrue(userId)) {
            schedule.setActive(false);
            scheduleRepository.save(schedule);
        }
        for (RecurringExpenseEntity expense : recurringExpenseRepository.findByUserIdAndActiveTrue(userId)) {
            expense.setActive(false);
            recurringExpenseRepository.save(expense);
        }
    }

    private void upsertPersona(Long userId) {
        PersonaProfileEntity persona = personaProfileRepository.findByUserId(userId).orElseGet(PersonaProfileEntity::new);
        persona.setUserId(userId);
        persona.setOccupationCode("NEW_WORKER");
        persona.setResidenceCode("MONTHLY_RENT");
        persona.setHouseholdType("SINGLE");
        persona.setMonthlyIncomeAvg(2_450_000L);
        persona.setIncomeVariabilityPct(12.4);
        persona.setAge(27);
        persona.setSource("USER");
        persona.setUserOverridden(true);
        persona.setInferredAt(LocalDateTime.now());
        personaProfileRepository.save(persona);
    }

    private void seedSchedules(Long userId) {
        LocalDate today = LocalDate.now();
        List<CashflowScheduleEntity> schedules = List.of(
                schedule(userId, "CARD_DAY", "카드값", 520_000L, "EXPENSE", today.plusDays(7).getDayOfMonth()),
                schedule(userId, "SALARY_DAY", "월급", 2_450_000L, "INCOME", today.plusDays(14).getDayOfMonth()),
                schedule(userId, "RENT_DAY", "다음 월세", 750_000L, "EXPENSE", today.plusDays(26).getDayOfMonth()),
                schedule(userId, "LOAN_DAY", "학자금 대출", 180_000L, "EXPENSE", today.plusDays(19).getDayOfMonth()),
                schedule(userId, "UTIL_DAY", "관리비", 100_000L, "EXPENSE", today.plusDays(21).getDayOfMonth())
        );
        scheduleRepository.saveAll(schedules);
    }

    private CashflowScheduleEntity schedule(Long userId, String type, String label, long amount, String direction, int dayOfMonth) {
        CashflowScheduleEntity entity = new CashflowScheduleEntity();
        entity.setUserId(userId);
        entity.setScheduleTypeCode(type);
        entity.setLabel(label);
        entity.setAmount(amount);
        entity.setDirection(direction);
        entity.setDayOfMonth(Math.max(1, Math.min(28, dayOfMonth)));
        entity.setSource("DEMO");
        entity.setActive(true);
        return entity;
    }

    private void seedRecurringExpenses(Long userId) {
        recurringExpenseRepository.saveAll(List.of(
                recurring(userId, "FOOD", "식비/카페", 320_000L, LocalDate.now().plusDays(16).getDayOfMonth(), 0.82),
                recurring(userId, "COMM", "통신비", 70_000L, LocalDate.now().plusDays(11).getDayOfMonth(), 0.95),
                recurring(userId, "SUBSCRIPTION", "구독료", 29_000L, LocalDate.now().plusDays(9).getDayOfMonth(), 0.9)
        ));
    }

    private RecurringExpenseEntity recurring(Long userId, String category, String name, long amount, int dayOfMonth, double confidence) {
        RecurringExpenseEntity entity = new RecurringExpenseEntity();
        entity.setUserId(userId);
        entity.setExpenseCategoryCode(category);
        entity.setName(name);
        entity.setAvgAmount(amount);
        entity.setDayOfMonth(Math.max(1, Math.min(28, dayOfMonth)));
        entity.setConfidence(confidence);
        entity.setOccurrenceCount(3);
        entity.setLastSeenAt(LocalDateTime.now());
        entity.setActive(true);
        return entity;
    }

    private void seedSettings(String userId) {
        upsertSetting(userId, "OPENING_BALANCE_OVERRIDE", 610_000L);
        upsertSetting(userId, "SAFETY_BALANCE", 700_000L);
        upsertSetting(userId, "MONTHLY_VARIABLE_BUDGET", 240_000L);
    }

    private void upsertSetting(String userId, String key, long value) {
        UserSettingEntity setting = userSettingRepository.findByUserIdAndSettingKey(userId, key)
                .orElseGet(UserSettingEntity::new);
        setting.setUserId(userId);
        setting.setSettingKey(key);
        setting.setSettingValueLong(value);
        setting.setSettingValueStr(null);
        userSettingRepository.save(setting);
    }
}
