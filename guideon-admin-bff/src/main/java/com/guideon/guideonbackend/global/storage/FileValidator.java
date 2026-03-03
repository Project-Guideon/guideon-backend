package com.guideon.guideonbackend.global.storage;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

public class FileValidator {

    private static final Set<String> ALLOWED_PDF_CONTENT_TYPES = Set.of("application/pdf");
    private static final Set<String> ALLOWED_PDF_EXTENSIONS = Set.of(".pdf");

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp"
    );

    public static void validatePdf(MultipartFile file) {
        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename();

        if (contentType == null || !ALLOWED_PDF_CONTENT_TYPES.contains(contentType)) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "PDF 파일만 업로드 가능합니다.");
        }
        if (originalName == null) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "파일명이 없습니다.");
        }
        String lowerName = originalName.toLowerCase();
        boolean validExt = ALLOWED_PDF_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!validExt) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "PDF 파일만 업로드 가능합니다.");
        }
    }

    public static void validateImage(MultipartFile file) {
        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename();

        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "JPG, PNG, WEBP 이미지만 업로드 가능합니다.");
        }
        if (originalName == null) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "파일명이 없습니다.");
        }
        String lowerName = originalName.toLowerCase();
        boolean validExt = ALLOWED_IMAGE_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!validExt) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "JPG, PNG, WEBP 이미지만 업로드 가능합니다.");
        }
    }

    public static String computeFileHash(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "파일 해시 계산에 실패했습니다: " + e.getMessage());
        }
    }

    public static String computeFileHash(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileBytes);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "파일 해시 계산에 실패했습니다: " + e.getMessage());
        }
    }

    private FileValidator() {}
}
