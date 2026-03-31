package org.example.service;

import org.example.model.Booking;
import org.example.model.Property;
import org.example.model.User;
import org.example.repository.BookingRepository;
import org.example.repository.PropertyRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock PropertyRepository propertyRepository;
    @Mock UserRepository userRepository;

    @InjectMocks BookingService bookingService;

    @Captor ArgumentCaptor<Booking> bookingCaptor;

    @Test
    void createBooking_setsRequestedStatusAndCopiesMonthlyRent() {
        User tenant = User.builder().id(10L).email("t@t").role("TENANT").build();
        Property property = Property.builder().id(20L).pricePerMonth(40000).build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(tenant));
        when(propertyRepository.findById(20L)).thenReturn(Optional.of(property));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Booking booking = bookingService.createBooking(10L, 20L, LocalDate.parse("2026-03-01"), LocalDate.parse("2026-04-01"));

        verify(bookingRepository).save(bookingCaptor.capture());
        Booking saved = bookingCaptor.getValue();

        assertThat(saved.getStatus()).isEqualTo("requested");
        assertThat(saved.getMonthlyRent()).isEqualTo(40000);
        assertThat(saved.getTenant()).isSameAs(tenant);
        assertThat(saved.getProperty()).isSameAs(property);

        assertThat(booking.getStatus()).isEqualTo("requested");
    }

    @Test
    void cancelBooking_setsCancelled() {
        Booking b = Booking.builder().id(1L).status("requested").build();
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(b));

        bookingService.cancelBooking(1L);

        assertThat(b.getStatus()).isEqualTo("cancelled");
        verify(bookingRepository).save(b);
    }
}

