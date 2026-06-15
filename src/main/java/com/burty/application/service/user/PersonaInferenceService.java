/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 애플리케이션 서비스 (PersonaInferenceService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.user
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
package com.burty.application.service.user;

import com.burty.application.port.in.user.PersonaInferenceUseCase;
import com.burty.application.port.out.mydata.MyDataPort;
import com.burty.core.constant.LogMessages;
import com.burty.domain.asset.model.AssetSnapshot;
import com.burty.domain.user.entity.PersonaProfileEntity;
import com.burty.domain.user.repository.PersonaProfileRepository;
import com.burty.util.PersonaHeuristics;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonaInferenceService implements PersonaInferenceUseCase {
  private static final Logger log = LoggerFactory.getLogger(PersonaInferenceService.class);

  private final PersonaProfileRepository repository;
  private final MyDataPort myDataPort;
  private final PersonaHeuristics personaHeuristics;

  public PersonaInferenceService(
      PersonaProfileRepository repository,
      MyDataPort myDataPort,
      PersonaHeuristics personaHeuristics) {
    this.repository = repository;
    this.myDataPort = myDataPort;
    this.personaHeuristics = personaHeuristics;
  }

  @Override
  @Transactional
  public PersonaProfileEntity getOrInfer(String userId) {
    Long numericUserId = parseUserId(userId);
    if (numericUserId == null) {
      return inferTransient(userId);
    }
    return repository
        .findByUserId(numericUserId)
        .orElseGet(() -> persistInferred(numericUserId, userId));
  }

  @Override
  @Transactional
  public PersonaProfileEntity overrideByUser(
      String userId,
      String occupationCode,
      String residenceCode,
      String householdType,
      Long monthlyIncomeAvg) {
    Long numericUserId = parseUserId(userId);
    if (numericUserId == null) throw new IllegalArgumentException("userId must be numeric");
    PersonaProfileEntity entity =
        repository
            .findByUserId(numericUserId)
            .orElseGet(
                () -> {
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
    PersonaProfileEntity entity =
        repository.findByUserId(numericUserId).orElseGet(PersonaProfileEntity::new);
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
    log.info(
        LogMessages.User.PERSONA_INFERRED,
        userId,
        entity.getOccupationCode(),
        entity.getResidenceCode(),
        entity.getMonthlyIncomeAvg());
    return repository.save(entity);
  }

  private PersonaProfileEntity inferTransient(String userId) {
    PersonaProfileEntity entity = new PersonaProfileEntity();
    applyInferred(entity, userId);
    return entity;
  }

  private void applyInferred(PersonaProfileEntity entity, String userId) {
    AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
    entity.setOccupationCode(personaHeuristics.inferOccupationCode(snapshot));
    entity.setMonthlyIncomeAvg(personaHeuristics.estimateMonthlyIncome(snapshot));
    entity.setIncomeVariabilityPct(snapshot.volatilityPercent());
    entity.setAge(personaHeuristics.inferAgeFromUserId(userId));
    if (entity.getResidenceCode() == null) entity.setResidenceCode("MONTHLY_RENT");
    if (entity.getHouseholdType() == null) entity.setHouseholdType("SINGLE");
    entity.setSource("INFERRED");
    entity.setInferredAt(LocalDateTime.now());
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
