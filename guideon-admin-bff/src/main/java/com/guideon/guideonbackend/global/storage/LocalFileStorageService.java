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

    public LocalFileStorageService(
            @Value("${file.upload-dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(Long siteId, String fileHash, MultipartFile file) {
        try {
            Path siteDir = uploadDir.resolve(String.valueOf(siteId));
            Files.createDirectories(siteDir);

            String extension = extractExtension(file.getOriginalFilename());
            Path targetPath = siteDir.resolve(fileHash + extension);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("파일 저장 완료: {}", targetPath);

            return targetPath.toString();
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
            Path targetPath = siteDir.resolve(fileHash + extension);

            Files.write(targetPath, fileBytes);
            log.info("파일 저장 완료 (base64): {}", targetPath);

            return targetPath.toString();
        } catch (IOException e) {
            log.error("파일 저장 실패 (base64): siteId={}, fileHash={}", siteId, fileHash, e);
            throw new CustomException(ErrorCode.DOC_UPLOAD_FAILED,
                    "파일 저장에 실패했습니다: " + e.getMessage());
        }
    }

    private String extractExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        return ".pdf";
    }
}
