package org.example.sportstadiumbookingsystem.controller;

import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.stadiumimage.StadiumImageResponse;
import org.example.sportstadiumbookingsystem.service.StadiumImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/stadiums/{stadiumId}/images")
@RequiredArgsConstructor
public class StadiumImageController {

    private final StadiumImageService stadiumImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STADIUM_OWNER')")
    public ResponseEntity<List<StadiumImageResponse>> uploadImages(
            @PathVariable Long stadiumId,
            @RequestParam("files") List<MultipartFile> files) {

        List<StadiumImageResponse> response = stadiumImageService.uploadImages(stadiumId, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<StadiumImageResponse>> getImages(@PathVariable Long stadiumId) {
        return ResponseEntity.ok(stadiumImageService.getImages(stadiumId));
    }

    @PutMapping("/{imageId}/primary")
    @PreAuthorize("hasRole('STADIUM_OWNER')")
    public ResponseEntity<StadiumImageResponse> setPrimaryImage(
            @PathVariable Long stadiumId,
            @PathVariable Long imageId) {

        return ResponseEntity.ok(stadiumImageService.setPrimaryImage(stadiumId, imageId));
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('STADIUM_OWNER')")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long stadiumId,
            @PathVariable Long imageId) {

        stadiumImageService.deleteImage(stadiumId, imageId);
        return ResponseEntity.noContent().build();
    }
}