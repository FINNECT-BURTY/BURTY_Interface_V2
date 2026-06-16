/**
 *
 *
 * <pre>
 * <b>Description  : 유틸 (StoredFileResult)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.util
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
package com.burty.util;

/** FileUtil 저장 결과. {@code filePath} 는 upload 루트 기준 상대 경로입니다. */
public record StoredFileResult(
    String storedFilename, String originalFilename, String filePath, long fileSize) {}
