package com.guideon.guideonbackend.global.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장 서비스 인터페이스
 * Local / S3 등 구현체를 교체 가능
 */
public interface FileStorageService {

    String store(Long siteId, String fileHash, MultipartFile file);

    String store(Long siteId, String fileHash, byte[] fileBytes, String originalName);
}
