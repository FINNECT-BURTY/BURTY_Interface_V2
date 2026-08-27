package com.burty.core.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 페이지 응답 표준 형태.
 *
 * <p>Spring 의 {@code Page} 를 그대로 직렬화하면 내부 구조(pageable, sort 등)가 API 계약으로 새어나가고, Spring 버전 업그레이드 때
 * 응답 형태가 조용히 바뀐다. 필요한 필드만 명시적으로 노출한다.
 */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages, boolean hasNext) {

  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.hasNext());
  }
}
