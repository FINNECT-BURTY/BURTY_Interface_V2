package com.burty.application.service;

import com.burty.application.port.in.MyDataAuthUseCase;
import com.burty.application.port.out.AuditLogPort;
import com.burty.application.port.out.MyDataOAuthPort;
import com.burty.domain.entity.MyDataLinkStatusEntity;
import com.burty.domain.model.AuditEvent;
import com.burty.domain.repository.MyDataLinkStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MyDataAuthService implements MyDataAuthUseCase {
    private static final Logger log = LoggerFactory.getLogger(MyDataAuthService.class);
    private static final String DEFAULT_INSTITUTION = "MYDATA";

    private final MyDataOAuthPort myDataOAuthPort;
    private final MyDataLinkStatusRepository linkStatusRepository;
    private final AuditLogPort auditLogPort;

    public MyDataAuthService(MyDataOAuthPort myDataOAuthPort, MyDataLinkStatusRepository linkStatusRepository, AuditLogPort auditLogPort) {
        this.myDataOAuthPort = myDataOAuthPort;
        this.linkStatusRepository = linkStatusRepository;
        this.auditLogPort = auditLogPort;
    }

    @Override
    public String createAuthorizeUrl(String userId) {
        return createAuthorizeUrl(userId, DEFAULT_INSTITUTION);
    }

    @Override
    public String createAuthorizeUrl(String userId, String institutionCode) {
        String inst = nullOrBlank(institutionCode) ? DEFAULT_INSTITUTION : institutionCode;
        String stateUserId = inst.equals(DEFAULT_INSTITUTION) ? userId : userId + "::" + inst;
        return myDataOAuthPort.buildAuthorizeUrl(stateUserId);
    }

    @Override
    @Transactional
    public boolean exchangeAuthorizationCode(String userId, String code) {
        return exchangeAuthorizationCode(userId, DEFAULT_INSTITUTION, code);
    }

    @Override
    @Transactional
    public boolean exchangeAuthorizationCode(String userId, String institutionCode, String code) {
        String inst = nullOrBlank(institutionCode) ? DEFAULT_INSTITUTION : institutionCode;
        try {
            String accessToken = myDataOAuthPort.exchangeCodeForAccessToken(userId, code);
            boolean success = accessToken != null && !accessToken.isBlank();
            recordStatus(userId, inst, success ? "ACTIVE" : "FAILED", success ? null : "EMPTY_TOKEN");
            auditLogPort.save(new AuditEvent(
                    UUID.randomUUID().toString(), userId,
                    "MYDATA_TOKEN_EXCHANGE", inst,
                    success ? "SUCCESS" : "FAILURE",
                    success ? "linked" : "no token returned",
                    LocalDateTime.now()
            ));
            return success;
        } catch (Exception e) {
            log.warn("MyData token exchange failed userId={} institution={} err={}", userId, inst, e.getMessage());
            recordStatus(userId, inst, "FAILED", classify(e));
            auditLogPort.save(new AuditEvent(
                    UUID.randomUUID().toString(), userId,
                    "MYDATA_TOKEN_EXCHANGE", inst,
                    "FAILURE",
                    "exception=" + e.getClass().getSimpleName(),
                    LocalDateTime.now()
            ));
            return false;
        }
    }

    @Override
    public List<MyDataLinkStatusEntity> listInstitutions(String userId) {
        return linkStatusRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public boolean unlinkInstitution(String userId, String institutionCode) {
        String inst = nullOrBlank(institutionCode) ? DEFAULT_INSTITUTION : institutionCode;
        return linkStatusRepository.findByUserIdAndInstitutionCode(userId, inst)
                .map(entity -> {
                    entity.setStatus("UNLINKED");
                    entity.setUnlinkedAt(LocalDateTime.now());
                    linkStatusRepository.save(entity);
                    auditLogPort.save(new AuditEvent(
                            UUID.randomUUID().toString(), userId,
                            "MYDATA_UNLINK", inst, "SUCCESS",
                            "institutionCode=" + inst,
                            LocalDateTime.now()
                    ));
                    return true;
                })
                .orElse(false);
    }

    private void recordStatus(String userId, String institutionCode, String status, String errorCode) {
        MyDataLinkStatusEntity entity = linkStatusRepository
                .findByUserIdAndInstitutionCode(userId, institutionCode)
                .orElseGet(MyDataLinkStatusEntity::new);
        entity.setUserId(userId);
        entity.setInstitutionCode(institutionCode);
        entity.setStatus(status);
        if ("ACTIVE".equals(status)) {
            entity.setLinkedAt(LocalDateTime.now());
            entity.setUnlinkedAt(null);
            entity.setLastErrorCode(null);
            entity.setLastErrorAt(null);
            LocalDateTime expiresAt = myDataOAuthPort.findTokenExpiresAt(userId);
            if (expiresAt != null) entity.setTokenExpiresAt(expiresAt);
        } else {
            entity.setLastErrorCode(errorCode);
            entity.setLastErrorAt(LocalDateTime.now());
        }
        linkStatusRepository.save(entity);
    }

    private boolean nullOrBlank(String s) {
        return s == null || s.isBlank();
    }

    private String classify(Exception e) {
        String simple = e.getClass().getSimpleName();
        if (simple.contains("Timeout")) return "TIMEOUT";
        if (simple.contains("Unauthorized") || simple.contains("Forbidden")) return "AUTH";
        return "UNKNOWN";
    }
}
