package org.example.sportstadiumbookingsystem.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private static final String URL_PREFIX = "/uploads/stadium-images/";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    private static final Tika TIKA = new Tika();

    @Value("${app.upload.dir:uploads/stadium-images}")
    private String uploadDir;

    public String store(MultipartFile file) {
        String detectedType = validateFileType(file);
        validateFileSize(file);

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String storedFilename = UUID.randomUUID() + getExtensionForType(detectedType);
            Path targetPath = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath);

            return URL_PREFIX + storedFilename;
        } catch (IOException e) {
            log.error("Failed to store uploaded file", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store uploaded file");
        }
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(URL_PREFIX)) {
            return;
        }
        try {
            String filename = imageUrl.substring(URL_PREFIX.length());
            Path path = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(filename);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete stored file for {}", imageUrl, e);
        }
    }

    // ─── Validation (يقابل validateFileType()/validateFileSize() بالـ Class Diagram) ──

    private String validateFileType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file");
        }

        String detectedType = TIKA.detect(bytes);
        if (!ALLOWED_CONTENT_TYPES.contains(detectedType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only JPEG, PNG, and WEBP images are allowed");
        }
        return detectedType;
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size must not exceed 5MB");
        }
    }

    private String getExtensionForType(String detectedType) {
        return switch (detectedType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }
}