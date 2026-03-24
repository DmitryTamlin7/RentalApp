package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.DocumentDetailsDto;
import org.example.repository.UserRepository;
import org.example.service.DocumentService;
import org.example.service.DocumentService.DownloadStream;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.security.Principal;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TENANT') or hasRole('LANDLORD') or hasRole('ADMIN')")
public class DocumentController {

    private final DocumentService documentService;
    private final UserRepository userRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDetailsDto> uploadWithPrincipal(
            @RequestParam("title") String title,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "bookingId", required = false) Long bookingId,
            @RequestParam(value = "propertyId", required = false) Long propertyId,
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) {
        Long userId = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        DocumentDetailsDto dto = documentService.uploadDocument(
                userId,
                title,
                documentType,
                bookingId,
                propertyId,
                file
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public Page<DocumentDetailsDto> list(
            @RequestParam(value = "bookingId", required = false) Long bookingId,
            @RequestParam(value = "propertyId", required = false) Long propertyId,
            @RequestParam(value = "documentType", required = false) String documentType,
            @RequestParam(value = "q", required = false) String q,
            org.springframework.data.domain.Pageable pageable,
            Principal principal
    ) {
        Long userId = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        return documentService.searchDocuments(
                userId,
                bookingId,
                propertyId,
                documentType,
                q,
                pageable
        );
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<StreamingResponseBody> download(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        Long userId = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        DownloadStream ds = documentService.getDownloadStream(userId, id);

        String fileName = ds.fileName();
        String mimeType = ds.mimeType() == null ? "application/octet-stream" : ds.mimeType();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mimeType));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build());
        if (ds.size() != null) {
            headers.setContentLength(ds.size());
        }

        StreamingResponseBody body = outputStream -> {
            try (InputStream in = ds.stream()) {
                in.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .headers(headers)
                .body(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDetailsDto> details(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        Long userId = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        return ResponseEntity.ok(documentService.getDetails(userId, id));
    }
}

