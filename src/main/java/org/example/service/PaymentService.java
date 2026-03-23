package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.model.Booking;
import org.example.model.Payment;
import org.example.repository.BookingRepository;
import org.example.repository.PaymentRepository;
import org.example.repository.UserRepository;
import org.example.dto.PaymentDetailsDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
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

    public List<PaymentDetailsDto> getPaymentDetailsByTenantId(Long tenantId) {
        return paymentRepository.findByBooking_Tenant_Id(tenantId).stream()
                .map(this::toPaymentDetailsDto)
                .collect(Collectors.toList());
    }

    public List<Payment> getPaymentsByBookingAndLandlord(Long bookingId, Long landlordId) {
        return paymentRepository.findByBookingIdAndBooking_Property_Owner_Id(bookingId, landlordId);
    }

    public List<PaymentDetailsDto> getPaymentDetailsByBookingAndLandlord(Long bookingId, Long landlordId) {
        return paymentRepository.findByBookingIdAndBooking_Property_Owner_Id(bookingId, landlordId).stream()
                .map(this::toPaymentDetailsDto)
                .collect(Collectors.toList());
    }

    private PaymentDetailsDto toPaymentDetailsDto(Payment payment) {
        Booking booking = payment.getBooking();

        return new PaymentDetailsDto(
                payment.getId(),
                payment.getAmount(),
                payment.getDescription(),
                payment.getStatus(),
                payment.getRequestedAt(),
                payment.getTenantPaidAt(),
                payment.getConfirmedAt(),

                booking.getId(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getMonthlyRent(),

                booking.getTenant() != null ? booking.getTenant().getFullName() : null,

                booking.getProperty() != null ? booking.getProperty().getId() : null,
                booking.getProperty() != null ? booking.getProperty().getAddress() : null,
                booking.getProperty() != null ? booking.getProperty().getDescription() : null
        );
    }

    private Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }
}