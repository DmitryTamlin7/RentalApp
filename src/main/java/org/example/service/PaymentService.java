package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.model.Booking;
import org.example.model.Payment;
import org.example.repository.BookingRepository;
import org.example.repository.PaymentRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public Payment createPaymentRequest(Long landlordId, Long bookingId, BigDecimal amount, String description) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getProperty().getOwner().getId().equals(landlordId)) {
            throw new RuntimeException("Not your booking");
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(amount)
                .description(description)
                .status("pending")
                .requestedAt(LocalDateTime.now())
                .createdBy(userRepository.getReferenceById(landlordId))
                .build();

        return paymentRepository.save(payment);
    }

    public void markAsPaidByTenant(Long paymentId, Long tenantId) {
        Payment payment = getPayment(paymentId);
        if (!payment.getBooking().getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Not your payment");
        }
        if (!"pending".equals(payment.getStatus())) {
            throw new RuntimeException("Payment already processed");
        }
        payment.setStatus("tenant_paid");
        payment.setTenantPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    public void confirmPayment(Long paymentId, Long landlordId) {
        Payment payment = getPayment(paymentId);
        if (!payment.getBooking().getProperty().getOwner().getId().equals(landlordId)) {
            throw new RuntimeException("Not your payment");
        }
        if (!"tenant_paid".equals(payment.getStatus())) {
            throw new RuntimeException("Tenant has not paid yet");
        }
        payment.setStatus("confirmed");
        payment.setConfirmedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    public List<Payment> getPaymentsByTenantId(Long tenantId) {
        return paymentRepository.findByBooking_Tenant_Id(tenantId);
    }

    public List<Payment> getPaymentsByBookingAndLandlord(Long bookingId, Long landlordId) {
        return paymentRepository.findByBookingIdAndBooking_Property_Owner_Id(bookingId, landlordId);
    }

    private Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }
}