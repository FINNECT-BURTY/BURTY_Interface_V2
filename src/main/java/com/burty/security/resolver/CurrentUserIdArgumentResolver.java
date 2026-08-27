package com.burty.security.resolver;

import com.burty.core.annotation.CurrentUserId;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link CurrentUserId} 파라미터에 인증된 사용자 ID 를 주입한다.
 *
 * <p>값의 출처는 {@link SecurityContextHolder} 하나뿐이다. 요청 파라미터·헤더·바디는 참조하지 않는다.
 */
@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(CurrentUserId.class)
        && String.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    CurrentUserId annotation = parameter.getParameterAnnotation(CurrentUserId.class);
    boolean required = annotation == null || annotation.required();

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof String principal)
        || principal.isBlank()
        || "anonymousUser".equals(principal)) {
      if (required) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
      }
      return null;
    }
    return principal;
  }
}
