package com.berty.application.service;

import com.berty.domain.entity.PolicyEntity;
import com.berty.domain.repository.PolicyRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PolicySeeder {
    private static final Logger log = LoggerFactory.getLogger(PolicySeeder.class);

    private final PolicyRepository repository;

    public PolicySeeder(PolicyRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    @Transactional
    public void seed() {
        long inserted = 0;
        inserted += upsert(
                "seoul-youth-monthly-rent",
                "HOUSING",
                "서울 청년 월세 지원",
                "주거비",
                19, 39, 3_500_000L,
                "NEW_WORKER",
                "SEOUL",
                "월 최대 20만원, 최대 10개월 지원",
                "https://youth.seoul.go.kr/site/main/content/sh_housing_01",
                70
        );
        inserted += upsert(
                "seoul-youth-emergency-living",
                "FINANCE",
                "서울 청년 긴급생활비 지원",
                "생활안정",
                19, 39, 2_500_000L,
                "JOB_SEEKER",
                "SEOUL",
                "긴급 생활비 1회 최대 100만원 지원",
                "https://youth.seoul.go.kr/site/main/content/sh_emergency",
                75
        );
        inserted += upsert(
                "gov-youth-debt-adjustment",
                "FINANCE",
                "청년 부채 조정 컨설팅",
                "부채관리",
                19, 39, 4_500_000L,
                "FREELANCER",
                null,
                "신용회복위원회 청년 부채 조정 컨설팅",
                "https://www.ccrs.or.kr",
                65
        );
        log.info("Policy seeder inserted={} rows", inserted);
    }

    private long upsert(String code, String type, String title, String supportType,
                        int ageMin, int ageMax, long incomeMax,
                        String occupationCode, String residenceCode,
                        String benefit, String url, int priorityBase) {
        if (repository.existsById(code)) return 0;
        PolicyEntity p = new PolicyEntity();
        p.setPolicyCode(code);
        p.setPolicyTypeCode(type);
        p.setTitle(title);
        p.setSupportType(supportType);
        p.setAgeMin(ageMin);
        p.setAgeMax(ageMax);
        p.setIncomeMax(incomeMax);
        p.setOccupationCode(occupationCode);
        p.setResidenceCode(residenceCode);
        p.setBenefitSummary(benefit);
        p.setApplyUrl(url);
        p.setValidFrom(LocalDate.of(2026, 1, 1));
        p.setValidTo(LocalDate.of(2027, 12, 31));
        p.setActive(true);
        p.setPriorityBase(priorityBase);
        LocalDateTime now = LocalDateTime.now();
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        repository.saveAll(List.of(p));
        return 1;
    }
}
