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
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 업로드된 파일을 HTTP로 제공하는 내부 엔드포인트.
 * FastAPI가 storage_url을 통해 PDF를 다운로드할 때 사용.
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
        try {
            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize()
                    .resolve(String.valueOf(siteId))
                    .resolve(filename)
                    .normalize();

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("파일 다운로드 실패: siteId={}, filename={}", siteId, filename, e);
            return ResponseEntity.badRequest().build();
        }
    }
}
