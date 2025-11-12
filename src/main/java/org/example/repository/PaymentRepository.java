package org.example.repository;

import org.example.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBooking_Tenant_Id(Long tenantId);

    List<Payment> findByBookingIdAndBooking_Property_Owner_Id(
            Long bookingId, Long landlordId);
}