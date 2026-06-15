/**
 *
 *
 * <pre>
 * <b>Description  : 유틸 (FileUtil)</b>
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

import com.burty.core.config.FileStorageProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 로컬 파일 업로드·다운로드.
 *
 * <p>설정 단일 소스: {@link FileStorageProperties} ({@code file.storage.*}, env {@code UPLOAD_PATH}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileUtil {

  private final FileStorageProperties properties;

  public List<StoredFileResult> saveFiles(List<MultipartFile> files, String subDir) {
    List<StoredFileResult> results = new ArrayList<>();
    Path uploadDir = resolveUploadDir(subDir);
    ensureDirectory(uploadDir);

    for (MultipartFile file : files) {
      results.add(saveValidated(file, uploadDir));
    }
    return results;
  }

  public StoredFileResult saveFile(MultipartFile file, String subDir) {
    Path uploadDir = resolveUploadDir(subDir, todayFolder());
    ensureDirectory(uploadDir);
    return saveValidated(file, uploadDir, properties.getContractMaxFileSize());
  }

  public String saveImageFile(MultipartFile file, String subDir, String filePrefix) {
    if (file == null || file.isEmpty()) {
      return null;
    }
    validateSize(file, properties.getMaxFileSize());

    String originalFilename = sanitizeFilename(file.getOriginalFilename());
    String extension = extensionOf(originalFilename);
    if (!isAllowedExtension(extension)) {
      throw new BusinessException(ErrorCode.INVALID_FILE_TYPE, "지원하지 않는 이미지 형식입니다.");
    }

    Path uploadDir = resolveUploadDir(subDir);
    ensureDirectory(uploadDir);

    String newFileName = (filePrefix != null ? filePrefix + "_" : "") + originalFilename;
    Path target = uploadDir.resolve(newFileName);
    transfer(file, target);

    Path relative = relativize(target);
    return "/" + relative.toString().replace("\\", "/");
  }

  public boolean deleteFile(String storedPath) {
    if (!StringUtils.hasText(storedPath)) {
      return false;
    }
    try {
      Path path = resolveStoredPath(storedPath);
      if (Files.isRegularFile(path)) {
        return Files.deleteIfExists(path);
      }
    } catch (BusinessException e) {
      throw e;
    } catch (IOException e) {
      log.error("delete failed path={}", storedPath, e);
    }
    return false;
  }

  public Resource getDownloadResource(String storedPath) {
    Path path = resolveStoredPath(storedPath);
    if (!Files.exists(path) || !Files.isRegularFile(path)) {
      throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
    }
    return new FileSystemResource(path);
  }

  public ResponseEntity<Resource> createPreviewResponse(String storedPath, String fileName) {
    Resource resource = getDownloadResource(storedPath);
    String name = StringUtils.hasText(fileName) ? fileName : "file";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition("inline", name))
        .header(HttpHeaders.CONTENT_TYPE, contentType(name))
        .body(resource);
  }

  public ResponseEntity<Resource> createDownloadResponse(String storedPath, String fileName) {
    Resource resource = getDownloadResource(storedPath);
    String name = StringUtils.hasText(fileName) ? fileName : "download";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition("attachment", name))
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .body(resource);
  }

  private StoredFileResult saveValidated(MultipartFile file, Path uploadDir) {
    return saveValidated(file, uploadDir, properties.getMaxFileSize());
  }

  private StoredFileResult saveValidated(MultipartFile file, Path uploadDir, long maxSize) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "업로드 파일이 비어 있습니다.");
    }
    validateSize(file, maxSize);

    String originalFilename = sanitizeFilename(file.getOriginalFilename());
    String extension = extensionOf(originalFilename);
    if (!isAllowedExtension(extension)) {
      throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
    }

    String storedFilename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
    Path target = uploadDir.resolve(storedFilename);
    transfer(file, target);

    String relativePath = relativize(target).toString().replace("\\", "/");
    return new StoredFileResult(storedFilename, originalFilename, relativePath, file.getSize());
  }

  private void validateSize(MultipartFile file, long maxSize) {
    if (file.getSize() > maxSize) {
      throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
    }
  }

  private void transfer(MultipartFile file, Path target) {
    try {
      file.transferTo(target.toFile().getAbsoluteFile());
    } catch (IOException e) {
      log.error("upload failed target={}", target, e);
      throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
    }
  }

  /** upload 루트 기준 상대/절대 경로를 안전하게 해석합니다. */
  private Path resolveStoredPath(String storedPath) {
    if (!StringUtils.hasText(storedPath)) {
      throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
    }
    if (storedPath.contains("..")) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "잘못된 파일 경로입니다.");
    }

    Path base = uploadRoot().toAbsolutePath().normalize();
    Path candidate =
        Paths.get(storedPath).isAbsolute()
            ? Paths.get(storedPath).normalize()
            : base.resolve(storedPath.replace("/", File.separator)).normalize();

    if (!candidate.startsWith(base)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "허용되지 않은 파일 경로입니다.");
    }
    return candidate;
  }

  private Path relativize(Path absoluteTarget) {
    Path base = uploadRoot().toAbsolutePath().normalize();
    return base.relativize(absoluteTarget.toAbsolutePath().normalize());
  }

  private Path resolveUploadDir(String subDir) {
    return resolveUploadDir(subDir, null);
  }

  private Path resolveUploadDir(String subDir, String dateFolder) {
    Path base = uploadRoot();
    Path dir = StringUtils.hasText(subDir) ? base.resolve(subDir) : base;
    if (StringUtils.hasText(dateFolder)) {
      dir = dir.resolve(dateFolder);
    }
    return dir;
  }

  private Path uploadRoot() {
    return Paths.get(normalizeBaseDir());
  }

  private String normalizeBaseDir() {
    String dir = properties.getUploadDir();
    if (dir.endsWith("/")) {
      return dir.substring(0, dir.length() - 1);
    }
    return dir;
  }

  private static void ensureDirectory(Path dir) {
    try {
      Files.createDirectories(dir);
    } catch (IOException e) {
      throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "업로드 디렉터리 생성 실패");
    }
  }

  private static String todayFolder() {
    return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
  }

  private static String sanitizeFilename(String originalFilename) {
    if (!StringUtils.hasText(originalFilename)) {
      return "file";
    }
    return Paths.get(originalFilename).getFileName().toString();
  }

  private boolean isAllowedExtension(String extension) {
    if (!StringUtils.hasText(extension)) {
      return false;
    }
    String pattern = "." + extension;
    String allowed = properties.getAllowedExtensions();
    int index = allowed.indexOf(pattern);
    if (index == -1) {
      return false;
    }
    int next = index + pattern.length();
    return next >= allowed.length() || allowed.charAt(next) == '.';
  }

  private static String extensionOf(String originalFilename) {
    if (!StringUtils.hasText(originalFilename)) {
      return "";
    }
    int dot = originalFilename.lastIndexOf('.');
    return dot >= 0 ? originalFilename.substring(dot + 1).toLowerCase() : "";
  }

  private static String contentDisposition(String type, String fileName) {
    String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    return type + "; filename=\"" + encoded + "\"";
  }

  private static String contentType(String fileName) {
    return switch (extensionOf(fileName)) {
      case "pdf" -> "application/pdf";
      case "jpg", "jpeg" -> "image/jpeg";
      case "png" -> "image/png";
      case "gif" -> "image/gif";
      case "webp" -> "image/webp";
      case "txt" -> "text/plain; charset=utf-8";
      case "html" -> "text/html; charset=utf-8";
      case "xml" -> "application/xml";
      case "json" -> "application/json";
      default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
    };
  }
}
