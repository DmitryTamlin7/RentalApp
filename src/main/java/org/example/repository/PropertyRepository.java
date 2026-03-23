package org.example.repository;



import org.example.model.Property;
import org.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {
    List<Property> findByOwner(User owner);

    @Query("SELECT COUNT(p) FROM Property p WHERE p.owner.email = :email")
    long countByOwnerEmail(@Param("email") String email);

    @Query("SELECT COUNT(p) FROM Property p JOIN Booking b ON b.property = p " +
            "WHERE p.owner.email = :email AND b.status IN ('confirmed','active')")
    long countRentedByOwnerEmail(@Param("email") String email);
}
