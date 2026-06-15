/**
 *
 *
 * <pre>
 * <b>Description  : 파일 응답 DTO (FileUploadResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.file
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
package com.burty.application.dto.file;

import com.burty.util.StoredFileResult;

public record FileUploadResponse(
    String storedFilename,
    String originalFilename,
    String filePath,
    long fileSize,
    String category) {

  public static FileUploadResponse from(StoredFileResult result, String category) {
    return new FileUploadResponse(
        result.storedFilename(),
        result.originalFilename(),
        result.filePath(),
        result.fileSize(),
        category);
  }
}
