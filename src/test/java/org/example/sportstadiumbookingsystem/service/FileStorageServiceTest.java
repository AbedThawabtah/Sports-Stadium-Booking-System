package org.example.sportstadiumbookingsystem.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class FileStorageServiceTest {

    // Minimal but real magic-byte headers so Apache Tika detects the genuine type.
    private static final byte[] JPEG_BYTES = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10,
            'J', 'F', 'I', 'F', 0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
            (byte) 0xFF, (byte) 0xD9
    };

    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 'I', 'H', 'D', 'R'
    };

    private static final byte[] WEBP_BYTES = {
            'R', 'I', 'F', 'F', 0x1A, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
    };

    @Autowired
    private FileStorageService fileStorageService;

    private final List<String> storedUrls = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        storedUrls.forEach(fileStorageService::delete);
        storedUrls.clear();
    }

    private String store(MockMultipartFile file) {
        String url = fileStorageService.store(file);
        storedUrls.add(url);
        return url;
    }

    @Test
    void validJpeg_uploadSucceeds() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_BYTES);
        String url = store(file);
        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    void validPng_uploadSucceeds() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_BYTES);
        String url = store(file);
        assertTrue(url.endsWith(".png"));
    }

    @Test
    void validWebp_uploadSucceeds() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.webp", "image/webp", WEBP_BYTES);
        String url = store(file);
        assertTrue(url.endsWith(".webp"));
    }

    @Test
    void textFileSpoofedAsJpeg_isRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "This is not an image".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> fileStorageService.store(file));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void emptyFile_isRejected() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[0]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> fileStorageService.store(file));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void fileOver5MB_isRejected() {
        byte[] oversized = new byte[6 * 1024 * 1024];
        System.arraycopy(JPEG_BYTES, 0, oversized, 0, JPEG_BYTES.length);
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", oversized);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> fileStorageService.store(file));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason() != null && ex.getReason().contains("5MB"));
    }

    @Test
    void storedExtension_matchesDetectedTypeNotOriginalFilename() {
        MockMultipartFile mismatchedName = new MockMultipartFile("file", "file.png", "image/png", JPEG_BYTES);
        String url = store(mismatchedName);
        assertTrue(url.endsWith(".jpg"));

        MockMultipartFile noExtension = new MockMultipartFile("file", "file", "image/jpeg", JPEG_BYTES);
        String url2 = store(noExtension);
        assertTrue(url2.endsWith(".jpg"));
    }
}
