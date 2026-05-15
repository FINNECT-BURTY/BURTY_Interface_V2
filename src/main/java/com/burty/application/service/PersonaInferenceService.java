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
import java.util.UUID;

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
        UUID uuid = parseUuid(userId);
        if (uuid == null) {
            return inferTransient(userId);
        }
        return repository.findByUserId(uuid).orElseGet(() -> persistInferred(uuid, userId));
    }

    @Override
    @Transactional
    public PersonaProfileEntity overrideByUser(String userId, String occupationCode, String residenceCode, String householdType, Long monthlyIncomeAvg) {
        UUID uuid = parseUuid(userId);
        if (uuid == null) throw new IllegalArgumentException("userId must be a UUID");
        PersonaProfileEntity entity = repository.findByUserId(uuid).orElseGet(() -> {
            PersonaProfileEntity created = persistInferred(uuid, userId);
            return repository.findByUserId(uuid).orElse(created);
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
        UUID uuid = parseUuid(userId);
        if (uuid == null) return inferTransient(userId);
        PersonaProfileEntity entity = repository.findByUserId(uuid).orElseGet(PersonaProfileEntity::new);
        if (entity.getUserId() == null) entity.setUserId(uuid);
        applyInferred(entity, userId);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        return repository.save(entity);
    }

    private PersonaProfileEntity persistInferred(UUID uuid, String userId) {
        PersonaProfileEntity entity = new PersonaProfileEntity();
        entity.setUserId(uuid);
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

    private UUID parseUuid(String userId) {
        if (userId == null || userId.length() < 32) return null;
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
