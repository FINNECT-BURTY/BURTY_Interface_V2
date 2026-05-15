package com.burty.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 파일 저장 설정
 * Gemini, Runway 등 외부 API에서 생성된 이미지/동영상을 로컬에 저장하기 위한 설정
 */
@Component
@ConfigurationProperties(prefix = "file.storage")
@Getter
@Setter
public class FileStorageProperties {

    /**
     * 파일 저장 루트 디렉토리
     * 예: /Users/username/Desktop/HyperWise/uploads
     */
    private String uploadDir = "uploads";

    /**
     * 이미지 저장 하위 디렉토리
     */
    private String imageDir = "images";

    /**
     * 동영상 저장 하위 디렉토리
     */
    private String videoDir = "videos";

    /**
     * 인증서 저장 하위 디렉토리
     */
    private String certificateDir = "certificate";

    /**
     * 문서 저장 하위 디렉토리
     */
    private String documentDir = "documents";

    /**
     * 오디오 저장 하위 디렉토리
     */
    private String audioDir = "audio";

    /**
     * 최대 파일 크기 (바이트 단위, 기본 100MB)
     */
    private long maxFileSize = 104857600L; // 100MB
}
