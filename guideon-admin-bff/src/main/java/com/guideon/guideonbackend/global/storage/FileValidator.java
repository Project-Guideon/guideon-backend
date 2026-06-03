package com.guideon.guideonbackend.global.storage;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

public class FileValidator {

    private static final Set<String> ALLOWED_AUDIO_CONTENT_TYPES = Set.of(
            "audio/wav", "audio/wave", "audio/x-wav",
            "audio/mpeg", "audio/mp3", "audio/mp4", "audio/ogg", "audio/webm"
    );
    private static final Set<String> ALLOWED_AUDIO_EXTENSIONS = Set.of(
            ".wav", ".mp3", ".m4a", ".m4r", ".ogg", ".webm"
    );
    // MP3 등 비WAV 포맷: 320kbps 기준으로 크기 추정, 여유 2배 적용
    private static final int ASSUMED_MAX_BITRATE_BPS = 320_000;
    private static final double SIZE_SAFETY_FACTOR = 2.0;

    private static final Set<String> ALLOWED_PDF_CONTENT_TYPES = Set.of("application/pdf");
    private static final Set<String> ALLOWED_PDF_EXTENSIONS = Set.of(".pdf");

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp"
    );

    /**
     * 음성 샘플 파일 검증: 형식 + 10초 이하 길이 제한
     *
     * 스트림 이중 소비를 막기 위해 호출부에서 미리 읽은 byte[]를 받습니다.
     * - WAV: javax.sound.sampled 로 정확한 길이 계산
     * - MP3 등: 파일 크기로 근사 (320kbps × 10s × 2 = 800KB 이하)
     */
    public static void validateAudio(String originalName, byte[] audioBytes, double maxDurationSeconds) {
        if (audioBytes == null || audioBytes.length == 0) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "빈 파일은 업로드할 수 없습니다.");
        }
        if (originalName == null) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "파일명이 없습니다.");
        }

        String lowerName = originalName.toLowerCase();
        boolean validExt = ALLOWED_AUDIO_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!validExt) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR,
                    "지원하지 않는 오디오 형식입니다. WAV, MP3, M4A, M4R, OGG, WEBM만 가능합니다.");
        }

        if (lowerName.endsWith(".wav")) {
            // WAV: AudioSystem으로 정확한 재생 시간 계산
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(
                    new BufferedInputStream(new java.io.ByteArrayInputStream(audioBytes)))) {
                AudioFormat format = ais.getFormat();
                long frames = ais.getFrameLength();
                if (frames > 0 && format.getFrameRate() > 0) {
                    double duration = frames / format.getFrameRate();
                    if (duration > maxDurationSeconds) {
                        throw new CustomException(ErrorCode.VALIDATION_ERROR,
                                String.format("오디오가 너무 깁니다. %.0f초 이하로 업로드해 주세요. (현재: %.1f초)",
                                        maxDurationSeconds, duration));
                    }
                }
            } catch (CustomException e) {
                throw e;
            } catch (Exception e) {
                throw new CustomException(ErrorCode.VALIDATION_ERROR, "WAV 파일을 읽을 수 없습니다.");
            }
        } else {
            // MP3 등: 파일 크기 근사 검증 (320kbps × maxDurationSeconds × 2배 여유)
            long maxBytes = (long) ((ASSUMED_MAX_BITRATE_BPS / 8.0) * maxDurationSeconds * SIZE_SAFETY_FACTOR);
            if (audioBytes.length > maxBytes) {
                throw new CustomException(ErrorCode.VALIDATION_ERROR,
                        String.format("오디오 파일이 너무 큽니다. %.0f초 분량(약 %.0fKB) 이하로 업로드해 주세요.",
                                maxDurationSeconds, maxBytes / 1024.0));
            }
        }
    }

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

    /**
     * GLB(glTF Binary) 파일 검증: 매직바이트 + 크기 상한(50MB).
     * GLB 매직: 0x67 0x6C 0x54 0x46 ("glTF")
     * 50MB는 spring.servlet.multipart.max-file-size와 동일 — 그 이상은 Spring이 먼저 거부.
     */
    public static void validateGlb(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "빈 파일은 업로드할 수 없습니다.");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".glb")) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "GLB 파일만 업로드 가능합니다. (.glb 확장자 필요)");
        }
        if (file.getSize() > 50L * 1024 * 1024) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "GLB 파일은 50MB 이하만 업로드 가능합니다.");
        }
        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(4);
            if (!hasGlbSignature(header)) {
                throw new CustomException(ErrorCode.VALIDATION_ERROR, "손상되었거나 유효하지 않은 GLB 파일입니다.");
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

    private static boolean hasGlbSignature(byte[] header) {
        // glTF: 0x67 0x6C 0x54 0x46
        return header.length >= 4
                && header[0] == 0x67  // g
                && header[1] == 0x6C  // l
                && header[2] == 0x54  // T
                && header[3] == 0x46; // F
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
