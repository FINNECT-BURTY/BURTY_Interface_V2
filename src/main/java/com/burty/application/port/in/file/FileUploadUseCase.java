/**
 *
 *
 * <pre>
 * <b>Description  : 파일 유스케이스 포트 (FileUploadUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.file
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
package com.burty.application.port.in.file;

import com.burty.application.dto.file.FileUploadResponse;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadUseCase {

  FileUploadResponse upload(String category, MultipartFile file);

  List<FileUploadResponse> uploadAll(String category, List<MultipartFile> files);

  boolean delete(String filePath);

  ResponseEntity<Resource> download(String filePath, String fileName);

  ResponseEntity<Resource> preview(String filePath, String fileName);
}
