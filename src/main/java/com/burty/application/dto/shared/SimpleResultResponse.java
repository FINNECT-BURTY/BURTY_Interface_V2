/**
 *
 *
 * <pre>
 * <b>Description  : 공통 응답 DTO (SimpleResultResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.shared
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
package com.burty.application.dto.shared;

public record SimpleResultResponse(boolean success, String message) {}
