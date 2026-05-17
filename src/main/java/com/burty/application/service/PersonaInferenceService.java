package com.burty.application.service;

import com.burty.application.port.in.PersonaInferenceUseCase;
import com.burty.application.port.out.MyDataPort;
import com.burty.domain.entity.PersonaProfileEntity;
import com.burty.domain.model.AssetSnapshot;
import com.burty.domain.repository.PersonaProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PersonaInferenceService implements PersonaInferenceUseCase {
    private static final Logger log = LoggerFactory.getLogger(PersonaInferenceService.class);

    private final PersonaProfileRepository repository;
    private final MyDataPort myDataPort;

    public PersonaInferenceService(PersonaProfileRepository repository, MyDataPort myDataPort) {
        this.repository = repository;
        this.myDataPort = myDataPort;
    }

    @Override
    @Transactional
    public PersonaProfileEntity getOrInfer(String userId) {
        Long numericUserId = parseUserId(userId);
        if (numericUserId == null) {
            return inferTransient(userId);
        }
        return repository.findByUserId(numericUserId).orElseGet(() -> persistInferred(numericUserId, userId));
    }

    @Override
    @Transactional
    public PersonaProfileEntity overrideByUser(String userId, String occupationCode, String residenceCode, String householdType, Long monthlyIncomeAvg) {
        Long numericUserId = parseUserId(userId);
        if (numericUserId == null) throw new IllegalArgumentException("userId must be numeric");
        PersonaProfileEntity entity = repository.findByUserId(numericUserId).orElseGet(() -> {
            PersonaProfileEntity created = persistInferred(numericUserId, userId);
            return repository.findByUserId(numericUserId).orElse(created);
        });
        if (occupationCode != null) entity.setOccupationCode(occupationCode);
        if (residenceCode != null) entity.setResidenceCode(residenceCode);
        if (householdType != null) entity.setHouseholdType(householdType);
        if (monthlyIncomeAvg != null) entity.setMonthlyIncomeAvg(monthlyIncomeAvg);
        entity.setSource("USER");
        entity.setUserOverridden(true);
        return repository.save(entity);
    }

    @Override
    @Transactional
    public PersonaProfileEntity reinfer(String userId) {
        Long numericUserId = parseUserId(userId);
        if (numericUserId == null) return inferTransient(userId);
        PersonaProfileEntity entity = repository.findByUserId(numericUserId).orElseGet(PersonaProfileEntity::new);
        if (entity.getUserId() == null) entity.setUserId(numericUserId);
        applyInferred(entity, userId);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        return repository.save(entity);
    }

    private PersonaProfileEntity persistInferred(Long numericUserId, String userId) {
        PersonaProfileEntity entity = new PersonaProfileEntity();
        entity.setUserId(numericUserId);
        applyInferred(entity, userId);
        log.info("Persona inferred userId={} occupation={} residence={} income={}",
                userId, entity.getOccupationCode(), entity.getResidenceCode(), entity.getMonthlyIncomeAvg());
        return repository.save(entity);
    }

    private PersonaProfileEntity inferTransient(String userId) {
        PersonaProfileEntity entity = new PersonaProfileEntity();
        applyInferred(entity, userId);
        return entity;
    }

    private void applyInferred(PersonaProfileEntity entity, String userId) {
        AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
        entity.setOccupationCode(inferOccupation(snapshot));
        entity.setMonthlyIncomeAvg(estimateMonthlyIncome(snapshot));
        entity.setIncomeVariabilityPct(snapshot.getVolatilityPercent());
        entity.setAge(inferAgeFromUserId(userId));
        if (entity.getResidenceCode() == null) entity.setResidenceCode("MONTHLY_RENT");
        if (entity.getHouseholdType() == null) entity.setHouseholdType("SINGLE");
        entity.setSource("INFERRED");
        entity.setInferredAt(LocalDateTime.now());
    }

    private String inferOccupation(AssetSnapshot snapshot) {
        double volatility = snapshot.getVolatilityPercent();
        if (volatility > 20) return "FREELANCER";
        if (snapshot.getMonthlySpend() < 2_000_000) return "JOB_SEEKER";
        return "NEW_WORKER";
    }

    private long estimateMonthlyIncome(AssetSnapshot snapshot) {
        long spend = Math.round(snapshot.getMonthlySpend());
        return Math.max(1_000_000L, Math.round(spend * 1.1));
    }

    private int inferAgeFromUserId(String userId) {
        int base = Math.abs(userId.hashCode() % 10);
        return 24 + base;
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.isBlank()) return null;
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
