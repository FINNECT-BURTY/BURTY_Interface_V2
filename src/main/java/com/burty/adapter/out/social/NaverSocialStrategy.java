/**
 *
 *
 * <pre>
 * <b>Description  : 소셜로그인 (NaverSocialStrategy)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.social
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
package com.burty.adapter.out.social;

import com.burty.config.SocialLoginProperties;
import com.burty.domain.auth.model.SocialProfile;
import com.burty.domain.auth.model.SocialProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NaverSocialStrategy extends AbstractOAuthCodeStrategy {

  public NaverSocialStrategy(SocialLoginProperties properties, OAuthHttpClient httpClient) {
    super(properties, httpClient);
  }

  @Override
  public SocialProvider supports() {
    return SocialProvider.NAVER;
  }

  @Override
  public SocialProfile fetchProfile(String code, String redirectUri, String codeVerifier) {
    String accessToken = exchangeAccessToken(code, redirectUri, codeVerifier);
    Map<String, Object> response = userInfo(accessToken);
    Map<String, Object> body = mapOrEmpty(response.get("response"));
    return new SocialProfile(
        stringValue(body.get("id")), stringValue(body.get("email")), stringValue(body.get("name")));
  }
}
