package org.example.repository;


import org.example.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT b FROM Booking b " +
            "WHERE b.property.id = :propertyId " +
            "AND b.status = 'ACTIVE'")
    Optional<Booking> findActiveByPropertyId(@Param("propertyId") Long propertyId);

    List<Booking> findByPropertyOwnerId(Long ownerId);

    List<Booking> findByTenantId(Long id);
}

