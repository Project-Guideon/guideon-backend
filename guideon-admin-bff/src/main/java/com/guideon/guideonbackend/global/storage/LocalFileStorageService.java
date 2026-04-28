package com.guideon.guideonbackend.global.storage;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadDir;
    private final String fileBaseUrl;

    public LocalFileStorageService(
            @Value("${file.upload-dir:./uploads}") String uploadDir,
            @Value("${file.base-url:http://localhost:8081}") String fileBaseUrl) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.fileBaseUrl = fileBaseUrl;
    }

    @Override
    public String store(Long siteId, String fileHash, MultipartFile file) {
        try {
            Path siteDir = uploadDir.resolve(String.valueOf(siteId));
            Files.createDirectories(siteDir);

            String extension = extractExtension(file.getOriginalFilename());
            String filename = fileHash + extension;
            Path targetPath = siteDir.resolve(filename);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("파일 저장 완료: {}", targetPath);

            return fileBaseUrl + "/internal/files/" + siteId + "/" + filename;
        } catch (IOException e) {
            log.error("파일 저장 실패: siteId={}, fileHash={}", siteId, fileHash, e);
            throw new CustomException(ErrorCode.DOC_UPLOAD_FAILED,
                    "파일 저장에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public String store(Long siteId, String fileHash, byte[] fileBytes, String originalName) {
        try {
            Path siteDir = uploadDir.resolve(String.valueOf(siteId));
            Files.createDirectories(siteDir);

            String extension = extractExtension(originalName);
            String filename = fileHash + extension;
            Path targetPath = siteDir.resolve(filename);

            Files.write(targetPath, fileBytes);
            log.info("파일 저장 완료 (base64): {}", targetPath);

            return fileBaseUrl + "/internal/files/" + siteId + "/" + filename;
        } catch (IOException e) {
            log.error("파일 저장 실패 (base64): siteId={}, fileHash={}", siteId, fileHash, e);
            throw new CustomException(ErrorCode.DOC_UPLOAD_FAILED,
                    "파일 저장에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public byte[] loadBytes(Long siteId, String storageUrl) {
        if (storageUrl == null || storageUrl.isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "storageUrl이 비어있습니다.");
        }

        String filename = extractFilename(storageUrl, siteId);

        Path siteDir = uploadDir.resolve(String.valueOf(siteId)).normalize();
        Path filePath = siteDir.resolve(filename).normalize();

        if (!filePath.startsWith(siteDir)) {
            log.warn("파일 읽기 거부 - 허용된 디렉토리 외부: storageUrl={}", storageUrl);
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "허용되지 않은 파일 경로입니다.");
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("파일 읽기 실패: siteId={}, storageUrl={}", siteId, storageUrl, e);
            throw new CustomException(ErrorCode.NOT_FOUND, "저장된 파일을 찾을 수 없습니다: " + storageUrl);
        }
    }

    private String extractFilename(String storageUrl, Long siteId) {
        String marker = "/internal/files/" + siteId + "/";
        int idx = storageUrl.indexOf(marker);
        if (idx < 0) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR,
                    "이 사이트에 속하지 않는 파일 URL입니다.");
        }
        String filename = storageUrl.substring(idx + marker.length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "잘못된 파일명입니다.");
        }
        return filename;
    }

    @Override
    public void delete(String storageUrl) {
        try {
            Path filePath = Paths.get(storageUrl).normalize();
            if (!filePath.startsWith(uploadDir)) {
                log.warn("파일 삭제 거부 - 허용된 디렉토리 외부: storageUrl={}", storageUrl);
                return;
            }
            Files.deleteIfExists(filePath);
            log.info("파일 삭제 완료: {}", filePath);
        } catch (IOException e) {
            log.warn("파일 삭제 실패 (무시): storageUrl={}, error={}", storageUrl, e.getMessage());
        }
    }

    private String extractExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        return ".pdf";
    }
}
