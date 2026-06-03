package com.guideon.guideonbackend.global.storage;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/**
 * 파일 저장 서비스 인터페이스
 * Local / S3 등 구현체를 교체 가능
 */
public interface FileStorageService {

    String store(Long siteId, String fileHash, MultipartFile file);

    String store(Long siteId, String fileHash, byte[] fileBytes, String originalName);

    /**
     * 저장된 파일을 URL로부터 다시 읽어 byte[]로 반환.
     * 외부 API(Tripo 등)에 재전송할 때 사용.
     */
    byte[] loadBytes(Long siteId, String storageUrl);

    void delete(String storageUrl);

    /**
     * 서빙 URL → 로컬 파일시스템 Path 변환.
     * mesh-processor가 파일에 직접 접근할 때 사용.
     * 예: "http://admin-bff:8081/internal/files/1/abc.glb" → /app/uploads/1/abc.glb
     */
    Path toLocalPath(String storageUrl);

    /**
     * siteId + filename → 서빙 URL 조합.
     * mesh-processor 출력 파일의 URL 생성에 사용.
     */
    String resolveUrl(Long siteId, String filename);
}
