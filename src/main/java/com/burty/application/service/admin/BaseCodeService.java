/**
 *
 *
 * <pre>
 * <b>Description  : 관리 애플리케이션 서비스 (BaseCodeService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.admin
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
package com.burty.application.service.admin;

import com.burty.application.port.in.admin.BaseCodeUseCase;
import com.burty.core.constant.LogMessages;
import com.burty.domain.admin.entity.BaseCodeEntity;
import com.burty.domain.admin.repository.BaseCodeRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BaseCodeService implements BaseCodeUseCase {
  private static final Logger log = LoggerFactory.getLogger(BaseCodeService.class);

  private final BaseCodeRepository repository;
  private final Map<String, List<BaseCodeEntity>> cacheByGroup = new ConcurrentHashMap<>();
  private final Map<String, BaseCodeEntity> cacheById = new ConcurrentHashMap<>();

  public BaseCodeService(BaseCodeRepository repository) {
    this.repository = repository;
  }

  @PostConstruct
  public void warmUp() {
    reload();
  }

  @Override
  public synchronized void reload() {
    cacheByGroup.clear();
    cacheById.clear();
    List<BaseCodeEntity> all = repository.findAll();
    Map<String, List<BaseCodeEntity>> grouped =
        all.stream()
            .filter(e -> "Y".equalsIgnoreCase(e.getUseYn()))
            .collect(Collectors.groupingBy(BaseCodeEntity::getCodeGroup));
    grouped.forEach(
        (group, list) -> {
          list.sort(
              Comparator.comparing(
                  BaseCodeEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo)));
          cacheByGroup.put(group, List.copyOf(list));
        });
    all.forEach(e -> cacheById.put(e.getCodeId(), e));
    log.info(LogMessages.Admin.BASE_CODE_CACHE_LOADED, cacheByGroup.size(), all.size());
  }

  @Override
  public List<BaseCodeEntity> lookup(String codeGroup) {
    return cacheByGroup.getOrDefault(codeGroup, List.of());
  }

  @Override
  public Optional<BaseCodeEntity> lookup(String codeGroup, String codeValue) {
    return lookup(codeGroup).stream()
        .filter(e -> e.getCodeValue().equalsIgnoreCase(codeValue))
        .findFirst();
  }

  @Override
  public List<BaseCodeEntity> children(String parentCodeId) {
    return cacheByGroup.values().stream()
        .flatMap(List::stream)
        .filter(e -> parentCodeId.equals(e.getParentCodeId()))
        .sorted(
            Comparator.comparing(
                BaseCodeEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
        .toList();
  }

  @Override
  public String displayName(String codeGroup, String codeValue, String localeTag) {
    return lookup(codeGroup, codeValue)
        .map(
            e ->
                "en".equalsIgnoreCase(localeTag) && e.getCodeNameEn() != null
                    ? e.getCodeNameEn()
                    : e.getCodeNameKo())
        .orElse(codeValue);
  }

  @Override
  public BaseCodeEntity upsert(BaseCodeEntity entity, String operator) {
    LocalDateTime now = LocalDateTime.now();
    if (entity.getCodeId() == null || entity.getCodeId().isBlank()) {
      entity.setCodeId(entity.getCodeGroup() + "." + entity.getCodeValue());
    }
    if (entity.getUseYn() == null) entity.setUseYn("Y");
    if (entity.getSortOrder() == null) entity.setSortOrder(0);
    if (entity.getCreatedAt() == null) {
      entity.setCreatedAt(now);
      entity.setCreatedBy(operator);
    }
    entity.setUpdatedAt(now);
    entity.setUpdatedBy(operator);
    BaseCodeEntity saved = repository.save(entity);
    reload();
    return saved;
  }

  @Override
  public void deactivate(String codeId, String operator) {
    repository
        .findById(codeId)
        .ifPresent(
            e -> {
              e.setUseYn("N");
              e.setUpdatedAt(LocalDateTime.now());
              e.setUpdatedBy(operator);
              repository.save(e);
            });
    reload();
  }
}
