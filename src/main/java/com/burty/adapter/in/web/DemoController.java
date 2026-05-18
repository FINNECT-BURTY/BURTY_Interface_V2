package com.burty.adapter.in.web;

import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.entity.UserSettingEntity;
import com.burty.domain.repository.UserSettingRepository;
import com.burty.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth/demo")
@Tag(name = "BURTY Demo", description = "MVP 시연용 사용자와 현금흐름 데이터를 생성합니다.")
public class DemoController extends BaseController {
    private static final String DEMO_USER_ID = "demo-user";

    private final UserSettingRepository userSettingRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public DemoController(UserSettingRepository userSettingRepository,
                          JwtTokenProvider jwtTokenProvider) {
        this.userSettingRepository = userSettingRepository;
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
