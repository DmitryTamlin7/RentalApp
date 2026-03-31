package org.example.service;

import org.example.model.Booking;
import org.example.model.Payment;
import org.example.model.Property;
import org.example.model.User;
import org.example.repository.BookingRepository;
import org.example.repository.PaymentRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock BookingRepository bookingRepository;
    @Mock UserRepository userRepository;

    @InjectMocks PaymentService paymentService;

    @Captor ArgumentCaptor<Payment> paymentCaptor;

    @Test
    void createPaymentRequest_deniesIfNotYourBooking() {
        User owner = User.builder().id(1L).build();
        Property property = Property.builder().id(2L).owner(owner).build();
        Booking booking = Booking.builder().id(3L).property(property).build();

        when(bookingRepository.findById(3L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.createPaymentRequest(999L, 3L, BigDecimal.valueOf(100), "x"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not your booking");
    }

    @Test
    void markAsPaidByTenant_changesStatusToTenantPaid() {
        User tenant = User.builder().id(11L).build();
        Booking booking = Booking.builder().id(22L).tenant(tenant).property(Property.builder().owner(User.builder().id(33L).build()).build()).build();
        Payment payment = Payment.builder().id(44L).status("pending").booking(booking).build();

        when(paymentRepository.findById(44L)).thenReturn(Optional.of(payment));

        paymentService.markAsPaidByTenant(44L, 11L);

        assertThat(payment.getStatus()).isEqualTo("tenant_paid");
        assertThat(payment.getTenantPaidAt()).isNotNull();
        verify(paymentRepository).save(payment);
    }

    @Test
    void confirmPayment_deniesWhenTenantNotPaidYet() {
        User owner = User.builder().id(1L).build();
        Property property = Property.builder().owner(owner).build();
        Booking booking = Booking.builder().tenant(User.builder().id(2L).build()).property(property).build();
        Payment payment = Payment.builder().id(10L).status("pending").booking(booking).build();

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirmPayment(10L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant has not paid yet");
    }

    @Test
    void confirmPayment_setsConfirmed() {
        User owner = User.builder().id(1L).build();
        Property property = Property.builder().owner(owner).build();
        Booking booking = Booking.builder().tenant(User.builder().id(2L).build()).property(property).build();
        Payment payment = Payment.builder().id(10L).status("tenant_paid").booking(booking).build();

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        paymentService.confirmPayment(10L, 1L);

        assertThat(payment.getStatus()).isEqualTo("confirmed");
        assertThat(payment.getConfirmedAt()).isNotNull();
        verify(paymentRepository).save(payment);
    }
}

