package org.example.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.example.config.MinioConfig.MinioProperties;
import org.example.dto.DocumentDetailsDto;
import org.example.model.Booking;
import org.example.model.Document;
import org.example.model.Property;
import org.example.model.User;
import org.example.repository.BookingRepository;
import org.example.repository.DocumentRepository;
import org.example.repository.PropertyRepository;
import org.example.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "CONTRACT",
            "ACT",
            "UTILITY_BILL",
            "METER_REPORT",
            "OTHER"
    );

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    private final DocumentRepository documentRepository;
    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    @PostConstruct
    public void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure MinIO bucket exists", e);
        }
    }

    public Page<DocumentDetailsDto> searchDocuments(
            Long userId,
            Long bookingId,
            Long propertyId,
            String documentType,
            String q,
            Pageable pageable
    ) {
        String normalizedType = normalizeTypeOptional(documentType);

        return documentRepository.searchAccessible(
                        userId,
                        bookingId,
                        propertyId,
                        normalizedType,
                        q == null ? null : q.trim(),
                        pageable
                )
                .map(this::toDetailsDto);
    }

    public DocumentDetailsDto uploadDocument(
            Long userId,
            String title,
            String documentType,
            Long bookingId,
            Long propertyId,
            MultipartFile file
    ) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Название обязательно");
        }

        String normalizedType = normalizeTypeRequired(documentType);
        if (!ALLOWED_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("Неподдерживаемый тип");
        }

        if (bookingId == null && propertyId == null) {
        } else if (bookingId != null && propertyId != null) {
            throw new IllegalArgumentException("Only one of bookingId/propertyId can be set");
        }

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Booking booking = null;
        Property property = null;

        if (bookingId != null) {
            booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            boolean canUpload = (booking.getTenant() != null && booking.getTenant().getId().equals(userId))
                    || (booking.getProperty() != null && booking.getProperty().getOwner() != null && booking.getProperty().getOwner().getId().equals(userId));
            if (!canUpload) throw new RuntimeException("Access denied for booking document");
        }

        if (propertyId != null) {
            property = propertyRepository.findById(propertyId)
                    .orElseThrow(() -> new RuntimeException("Property not found"));
            if (property.getOwner() == null || !property.getOwner().getId().equals(userId)) {
                throw new RuntimeException("Access denied for property document");
            }
        }


        long size = file.getSize();
        if (size <= 0 || size > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must be 1..10MB");
        }

        String originalFileName = file.getOriginalFilename();
        String mimeType = file.getContentType();
        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "document";
        }
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("Неподдерживаемый тип. Нужен: PDF, JPG, PNG.");
        }


        String safeOriginal = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectKey = userId + "/" + UUID.randomUUID() + "_" + safeOriginal;

        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .stream(in, size, -1)
                            .contentType(mimeType)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("ОШибка загрузки MinIO", e);
        }

        Document doc = Document.builder()
                .title(title.trim())
                .documentType(normalizedType)
                .originalFileName(originalFileName)
                .objectKey(objectKey)
                .mimeType(mimeType)
                .size(size)
                .booking(booking)
                .property(property)
                .uploadedBy(uploader)
                .createdAt(LocalDateTime.now())
                .build();

        Document saved = documentRepository.save(doc);
        return toDetailsDto(saved);
    }

    public DocumentDetailsDto getDetails(Long userId, Long documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        validateAccess(userId, doc);
        return toDetailsDto(doc);
    }

    public DownloadStream getDownloadStream(Long userId, Long documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        validateAccess(userId, doc);

        String bucket = minioProperties.getBucket();
        try {
            InputStream stream = minioClient.getObject(
                    io.minio.GetObjectArgs.builder().bucket(bucket).object(doc.getObjectKey()).build()
            );
            return new DownloadStream(
                    doc.getOriginalFileName(),
                    doc.getMimeType(),
                    doc.getSize(),
                    stream
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to download from MinIO", e);
        }
    }

    private void validateAccess(Long userId, Document doc) {
        if (doc.getBooking() != null) {
            Booking booking = doc.getBooking();
            boolean can =
                    (booking.getTenant() != null && booking.getTenant().getId().equals(userId)) ||
                    (booking.getProperty() != null &&
                            booking.getProperty().getOwner() != null &&
                            booking.getProperty().getOwner().getId().equals(userId));
            if (!can) throw new RuntimeException("Access denied");
            return;
        }
        if (doc.getProperty() != null) {
            Property property = doc.getProperty();
            if (property.getOwner() == null || !property.getOwner().getId().equals(userId)) {
                throw new RuntimeException("Access denied");
            }
            return;
        }
        if (doc.getUploadedBy() == null || !doc.getUploadedBy().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
    }

    private DocumentDetailsDto toDetailsDto(Document doc) {
        Booking booking = doc.getBooking();
        Property property = doc.getProperty();

        String bookingTenantName = null;
        Long bookingId = null;
        if (booking != null) {
            bookingId = booking.getId();
            bookingTenantName = booking.getTenant() != null ? booking.getTenant().getFullName() : null;
        }

        String propertyAddress = null;
        Long propertyId = null;
        if (property != null) {
            propertyId = property.getId();
            propertyAddress = property.getAddress();
        }

        return new DocumentDetailsDto(
                doc.getId(),
                doc.getTitle(),
                doc.getDocumentType(),
                doc.getOriginalFileName(),
                doc.getMimeType(),
                doc.getSize(),
                doc.getCreatedAt(),
                bookingId,
                bookingTenantName,
                propertyAddress,
                propertyId
        );
    }

    private String normalizeTypeRequired(String documentType) {
        if (documentType == null || documentType.isBlank()) return "OTHER";
        return documentType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTypeOptional(String documentType) {
        if (documentType == null || documentType.isBlank()) return null;
        return documentType.trim().toUpperCase(Locale.ROOT);
    }

    public record DownloadStream(
            String fileName,
            String mimeType,
            Long size,
            InputStream stream
    ) {}
}

