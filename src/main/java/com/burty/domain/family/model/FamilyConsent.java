/**
 *
 *
 * <pre>
 * <b>Description  : 가족보호 도메인 모델 (FamilyConsent)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.family.model
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
package com.burty.domain.family.model;

public record FamilyConsent(String parentUserId, String childUserId, boolean consented) {}
