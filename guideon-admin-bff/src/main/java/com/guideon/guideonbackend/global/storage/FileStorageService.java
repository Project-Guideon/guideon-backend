package com.guideon.guideonbackend.global.storage;

import org.springframework.web.multipart.MultipartFile;

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
}
