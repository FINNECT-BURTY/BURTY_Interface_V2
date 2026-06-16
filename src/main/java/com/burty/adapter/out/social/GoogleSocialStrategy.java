/**
 *
 *
 * <pre>
 * <b>Description  : 소셜로그인 (GoogleSocialStrategy)</b>
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
public class GoogleSocialStrategy extends AbstractOAuthCodeStrategy {

  public GoogleSocialStrategy(SocialLoginProperties properties, OAuthHttpClient httpClient) {
    super(properties, httpClient);
  }

  @Override
  public SocialProvider supports() {
    return SocialProvider.GOOGLE;
  }

  @Override
  public SocialProfile fetchProfile(String code, String redirectUri, String codeVerifier) {
    String accessToken = exchangeAccessToken(code, redirectUri, codeVerifier);
    Map<String, Object> response = userInfo(accessToken);
    return new SocialProfile(
        stringValue(response.get("sub")),
        stringValue(response.get("email")),
        stringValue(response.get("name")));
  }
}
