/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 프로퍼티 (FileStorageProperties)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.config
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
package com.burty.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** BURTY 로컬 파일 저장 설정 ({@link com.burty.util.FileUtil}). */
@Component
@ConfigurationProperties(prefix = "file.storage")
@Getter
@Setter
public class FileStorageProperties {

  /** 업로드 루트 — Docker: {@code UPLOAD_PATH=/app/uploads}, 로컬: {@code uploads/} */
  private String uploadDir = "uploads";

  /** 이미지 저장 하위 디렉토리 */
  private String imageDir = "images";

  /** 동영상 저장 하위 디렉토리 */
  private String videoDir = "videos";

  /** 인증서 저장 하위 디렉토리 */
  private String certificateDir = "certificate";

  /** 문서 저장 하위 디렉토리 */
  private String documentDir = "documents";

  /** 오디오 저장 하위 디렉토리 */
  private String audioDir = "audio";

  /** 최대 파일 크기 (바이트 단위, 기본 100MB) */
  private long maxFileSize = 104857600L; // 100MB

  /** 계약/대용량 문서 업로드 한도 (기본 50MB) */
  private long contractMaxFileSize = 52428800L;

  /** 허용 확장자 — {@code .jpg.jpeg.png.pdf} 형식 */
  private String allowedExtensions = ".jpg.jpeg.png.gif.webp.pdf.txt.json.xml.doc.docx.xlsx.hwp";
}
