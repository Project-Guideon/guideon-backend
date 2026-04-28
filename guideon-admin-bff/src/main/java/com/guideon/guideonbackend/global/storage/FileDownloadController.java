package com.guideon.guideonbackend.global.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 업로드된 파일을 HTTP로 제공하는 엔드포인트.
 * - FastAPI가 storage_url을 통해 PDF를 다운로드할 때
 * - 프론트가 <img>로 업로드된 이미지를 렌더링할 때
 */
@Slf4j
@RestController
@RequestMapping("/internal/files")
@RequiredArgsConstructor
public class FileDownloadController {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @GetMapping("/{siteId}/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long siteId,
            @PathVariable String filename) {

        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = baseDir
                    .resolve(String.valueOf(siteId))
                    .resolve(filename)
                    .normalize();

            if (!filePath.startsWith(baseDir)) {
                log.warn("파일 다운로드 거부 - 허용된 디렉토리 외부: {}", filePath);
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            MediaType mediaType = resolveMediaType(filePath);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("파일 다운로드 실패: siteId={}, filename={}", siteId, filename, e);
            return ResponseEntity.badRequest().build();
        }
    }

    private MediaType resolveMediaType(Path filePath) {
        try {
            String probed = Files.probeContentType(filePath);
            if (probed != null) {
                return MediaType.parseMediaType(probed);
            }
        } catch (Exception ignored) {
        }

        String name = filePath.getFileName().toString().toLowerCase();
        if (name.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (name.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (name.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (name.endsWith(".glb")) return MediaType.parseMediaType("model/gltf-binary");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
