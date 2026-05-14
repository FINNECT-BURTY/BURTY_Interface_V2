package com.berty.application.port.in;

import com.berty.domain.entity.MyDataLinkStatusEntity;

import java.util.List;

public interface MyDataAuthUseCase {
    String createAuthorizeUrl(String userId);
    String createAuthorizeUrl(String userId, String institutionCode);
    boolean exchangeAuthorizationCode(String userId, String code);
    boolean exchangeAuthorizationCode(String userId, String institutionCode, String code);
    List<MyDataLinkStatusEntity> listInstitutions(String userId);
    boolean unlinkInstitution(String userId, String institutionCode);
}
