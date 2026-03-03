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
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "빈 파일은 업로드할 수 없습니다.");
        }
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
        try (InputStream is = file.getInputStream()) {
            if (!hasPdfSignature(is.readNBytes(5))) {
                throw new CustomException(ErrorCode.VALIDATION_ERROR, "손상되었거나 유효하지 않은 PDF 파일입니다.");
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "파일을 읽을 수 없습니다.");
        }
    }

    // Base64 업로드 경로용 오버로드 (content-type 없이 filename + 시그니처만 검증)
    public static void validatePdf(String originalName, byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "빈 파일은 업로드할 수 없습니다.");
        }
        if (originalName == null) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "파일명이 없습니다.");
        }
        String lowerName = originalName.toLowerCase();
        boolean validExt = ALLOWED_PDF_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!validExt) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "PDF 파일만 업로드 가능합니다.");
        }
        if (!hasPdfSignature(fileBytes)) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "손상되었거나 유효하지 않은 PDF 파일입니다.");
        }
    }

    public static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "빈 파일은 업로드할 수 없습니다.");
        }
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
        try (InputStream is = file.getInputStream()) {
            if (!hasImageSignature(is.readNBytes(12))) {
                throw new CustomException(ErrorCode.VALIDATION_ERROR, "손상되었거나 유효하지 않은 이미지 파일입니다.");
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "파일을 읽을 수 없습니다.");
        }
    }

    public static String computeFileHash(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "빈 파일은 업로드할 수 없습니다.");
        }
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "파일 해시 계산에 실패했습니다.");
        }
    }

    public static String computeFileHash(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "빈 파일은 업로드할 수 없습니다.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileBytes);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "파일 해시 계산에 실패했습니다.");
        }
    }

    private static boolean hasPdfSignature(byte[] header) {
        // %PDF-
        return header.length >= 5
                && header[0] == 0x25
                && header[1] == 0x50
                && header[2] == 0x44
                && header[3] == 0x46
                && header[4] == 0x2D;
    }

    private static boolean hasImageSignature(byte[] header) {
        return isJpeg(header) || isPng(header) || isWebP(header);
    }

    private static boolean isJpeg(byte[] h) {
        return h.length >= 3
                && (h[0] & 0xFF) == 0xFF
                && (h[1] & 0xFF) == 0xD8
                && (h[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] h) {
        return h.length >= 8
                && (h[0] & 0xFF) == 0x89
                && h[1] == 0x50   // P
                && h[2] == 0x4E   // N
                && h[3] == 0x47   // G
                && h[4] == 0x0D
                && h[5] == 0x0A
                && h[6] == 0x1A
                && h[7] == 0x0A;
    }

    private static boolean isWebP(byte[] h) {
        // RIFF....WEBP
        return h.length >= 12
                && h[0] == 0x52   // R
                && h[1] == 0x49   // I
                && h[2] == 0x46   // F
                && h[3] == 0x46   // F
                && h[8] == 0x57   // W
                && h[9] == 0x45   // E
                && h[10] == 0x42  // B
                && h[11] == 0x50; // P
    }

    private FileValidator() {}
}
