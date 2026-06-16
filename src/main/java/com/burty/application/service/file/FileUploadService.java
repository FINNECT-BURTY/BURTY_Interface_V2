/**
 *
 *
 * <pre>
 * <b>Description  : 파일 애플리케이션 서비스 (FileUploadService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.file
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
package com.burty.application.service.file;

import com.burty.application.dto.file.FileUploadResponse;
import com.burty.application.port.in.file.FileUploadUseCase;
import com.burty.core.config.FileStorageProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.util.FileUtil;
import com.burty.util.StoredFileResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileUploadService implements FileUploadUseCase {

  private final FileUtil fileUtil;
  private final FileStorageProperties properties;

  @Override
  public FileUploadResponse upload(String category, MultipartFile file) {
    String subDir = resolveSubDir(category);
    StoredFileResult saved = fileUtil.saveFile(file, subDir);
    return FileUploadResponse.from(saved, normalizeCategory(category));
  }

  @Override
  public List<FileUploadResponse> uploadAll(String category, List<MultipartFile> files) {
    if (files == null || files.isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "업로드할 파일이 없습니다.");
    }
    String subDir = resolveSubDir(category);
    String normalized = normalizeCategory(category);
    return fileUtil.saveFiles(files, subDir).stream()
        .map(result -> FileUploadResponse.from(result, normalized))
        .toList();
  }

  @Override
  public boolean delete(String filePath) {
    return fileUtil.deleteFile(filePath);
  }

  @Override
  public ResponseEntity<Resource> download(String filePath, String fileName) {
    return fileUtil.createDownloadResponse(filePath, fileName);
  }

  @Override
  public ResponseEntity<Resource> preview(String filePath, String fileName) {
    return fileUtil.createPreviewResponse(filePath, fileName);
  }

  private String resolveSubDir(String category) {
    if (!StringUtils.hasText(category)) {
      return properties.getDocumentDir();
    }
    return switch (category.trim().toUpperCase()) {
      case "IMAGE", "IMAGES" -> properties.getImageDir();
      case "VIDEO", "VIDEOS" -> properties.getVideoDir();
      case "AUDIO" -> properties.getAudioDir();
      case "CERTIFICATE", "CERT" -> properties.getCertificateDir();
      case "DOCUMENT", "DOCUMENTS", "DOC" -> properties.getDocumentDir();
      case "GENERAL", "ETC" -> "general";
      default ->
          throw new BusinessException(
              ErrorCode.INVALID_INPUT_VALUE,
              "지원하지 않는 category 입니다. (IMAGE, DOCUMENT, VIDEO, AUDIO, CERTIFICATE, GENERAL)");
    };
  }

  private static String normalizeCategory(String category) {
    return StringUtils.hasText(category) ? category.trim().toUpperCase() : "DOCUMENT";
  }
}
