/**
 *
 *
 * <pre>
 * <b>Description  : 파일 API 컨트롤러 (FileUploadController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.file
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
package com.burty.adapter.in.web.file;

import com.burty.application.dto.file.FileUploadResponse;
import com.burty.application.dto.shared.SimpleResultResponse;
import com.burty.application.port.in.file.FileUploadUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "BURTY Files", description = "파일 업로드·다운로드·미리보기 API")
public class FileUploadController extends BaseController {

  private final FileUploadUseCase fileUploadUseCase;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(
      summary = "단일 파일 업로드",
      description = "category: IMAGE | DOCUMENT | VIDEO | AUDIO | CERTIFICATE | GENERAL")
  public ApiResponse<FileUploadResponse> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam(defaultValue = "DOCUMENT") String category) {
    return ApiResponse.ok(fileUploadUseCase.upload(category, file));
  }

  @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "다중 파일 업로드")
  public ApiResponse<List<FileUploadResponse>> uploadBatch(
      @RequestParam("files") List<MultipartFile> files,
      @RequestParam(defaultValue = "DOCUMENT") String category) {
    return ApiResponse.ok(fileUploadUseCase.uploadAll(category, files));
  }

  @GetMapping("/download")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "파일 다운로드", description = "업로드 응답의 filePath 를 path 로 전달")
  public ResponseEntity<Resource> download(
      @RequestParam String path, @RequestParam(required = false) String filename) {
    return fileUploadUseCase.download(path, filename);
  }

  @GetMapping("/preview")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "파일 미리보기 (inline)")
  public ResponseEntity<Resource> preview(
      @RequestParam String path, @RequestParam(required = false) String filename) {
    return fileUploadUseCase.preview(path, filename);
  }

  @DeleteMapping
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "파일 삭제")
  public ApiResponse<SimpleResultResponse> delete(@RequestParam String path) {
    boolean deleted = fileUploadUseCase.delete(path);
    return ApiResponse.ok(
        new SimpleResultResponse(deleted, deleted ? "파일이 삭제되었습니다." : "파일을 찾을 수 없습니다."));
  }
}
