package org.example.controller;


import lombok.RequiredArgsConstructor;

import org.example.dto.PaymentRequest;
import org.example.dto.PaymentDetailsDto;
import org.example.model.Payment;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @PostMapping("/request")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<?> createPaymentRequest(
            @RequestBody PaymentRequest request,
            Principal principal) {

        User landlord = getCurrentUser(principal);
        Payment payment = paymentService.createPaymentRequest(
                landlord.getId(),
                request.bookingId(),
                request.amount(),
                request.description()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Запрос на оплату создан",
                "paymentId", payment.getId(),
                "amount", payment.getAmount()
        ));
    }

    @PostMapping("/{id}/tenant-paid")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<?> markAsPaid(@PathVariable Long id, Principal principal) {
        User tenant = getCurrentUser(principal);
        paymentService.markAsPaidByTenant(id, tenant.getId());
        return ResponseEntity.ok("Оплата отмечена");
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<?> confirmPayment(@PathVariable Long id, Principal principal) {
        User landlord = getCurrentUser(principal);
        paymentService.confirmPayment(id, landlord.getId());
        return ResponseEntity.ok("Оплата подтверждена");
    }


    @GetMapping("/my")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<List<PaymentDetailsDto>> getMyPayments(Principal principal) {
        User tenant = getCurrentUser(principal);
        return ResponseEntity.ok(paymentService.getPaymentDetailsByTenantId(tenant.getId()));
    }


    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<List<PaymentDetailsDto>> getPaymentsByBooking(
            @PathVariable Long bookingId, Principal principal) {
        User landlord = getCurrentUser(principal);
        return ResponseEntity.ok(paymentService.getPaymentDetailsByBookingAndLandlord(
                bookingId, landlord.getId()));
    }

    private User getCurrentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}