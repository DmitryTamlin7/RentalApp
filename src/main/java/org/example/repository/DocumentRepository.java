package org.example.repository;

import org.example.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query("""
    SELECT d FROM Document d
    LEFT JOIN d.booking b
    LEFT JOIN b.property bp
    LEFT JOIN d.property p
    WHERE
        (:bookingId IS NULL OR b.id = :bookingId)
        AND (:propertyId IS NULL OR p.id = :propertyId)
        AND (:documentType IS NULL OR LOWER(d.documentType) = LOWER(CAST(:documentType AS string)))
        AND (:q IS NULL OR :q = '' OR LOWER(d.title) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
        AND (
            d.uploadedBy.id = :userId
            OR (b IS NOT NULL AND (b.tenant.id = :userId OR bp.owner.id = :userId))
            OR (p IS NOT NULL AND p.owner.id = :userId)
        )
""")
    Page<Document> searchAccessible(
            Long userId,
            Long bookingId,
            Long propertyId,
            String documentType,
            String q,
            Pageable pageable
    );
    Optional<Document> findById(Long id);
}

