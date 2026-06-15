package com.burty.application.port.out.bank;

import com.burty.domain.mydata.model.MyDataTokenBundle;

/** 오픈뱅킹 OAuth (사용자 동의) 포트. */
public interface OpenBankingOAuthPort {

  String buildAuthorizeUrl(String oauthState);

  MyDataTokenBundle exchangeAuthorizationCode(String userId, String code);

  boolean isLinked(String userId);
}
