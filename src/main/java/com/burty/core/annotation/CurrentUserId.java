package com.burty.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 인증된 사용자 ID 를 주입한다.
 *
 * <p>왜 필요한가. 예전에는 컨트롤러가 {@code @RequestParam String userId} 로 <b>클라이언트가 보낸</b> 사용자 ID 를 받았다 (53개
 * 엔드포인트). 이걸 인터셉터가 {@code "userId"} 라는 파라미터 <b>이름 문자열</b>로 찾아 JWT subject 와 비교하는 방식으로 막고 있었다. 문제는:
 *
 * <ul>
 *   <li>파라미터명이 {@code targetUserId} 처럼 조금만 달라져도 방어가 조용히 뚫린다. 컴파일러도 테스트도 못 잡는다.
 *   <li>{@code @RequestBody} 안의 userId 는 아예 범위 밖이었다.
 *   <li>인증 정보가 없거나 principal 타입이 다르면 통과시키는 fail-open 이었다.
 * </ul>
 *
 * <p>이 애노테이션을 쓰면 값이 <b>항상 SecurityContext 에서만</b> 나온다. 클라이언트가 보낸 값은 쳐다보지도 않으므로, 위조할 대상 자체가 사라진다. 기존
 * 클라이언트가 {@code userId} 파라미터를 계속 보내도 무시될 뿐 오류는 아니다 (하위 호환).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUserId {

  /** false 로 두면 미인증 요청에서 null 이 주입된다 (공개 엔드포인트용). */
  boolean required() default true;
}
