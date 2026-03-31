package org.example.integration;

import io.minio.MinioClient;
import org.example.model.Property;
import org.example.repository.PropertyRepository;
import org.example.repository.UserRepository;
import org.example.testutil.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:rentaltestdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingPaymentFlowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PropertyRepository propertyRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @MockBean MinioClient minioClient; // DocumentService @PostConstruct

    private Long landlordId;
    private Long tenantId;
    private Long propertyId;

    @BeforeEach
    void init() throws Exception {
        // make DocumentService.ensureBucket not fail
        when(minioClient.bucketExists(any())).thenReturn(true);

        var landlord = TestUsers.ensureUser(userRepository, passwordEncoder, "landlord@test.com", "Pass123", "Land Lord", "LANDLORD");
        var tenant = TestUsers.ensureUser(userRepository, passwordEncoder, "tenant@test.com", "Pass123", "Ten Ant", "TENANT");
        landlordId = landlord.getId();
        tenantId = tenant.getId();

        Property property = Property.builder()
                .owner(landlord)
                .address("Москва Арбат 44")
                .description("2к квартира")
                .pricePerMonth(40000)
                .status("active")
                .build();
        property = propertyRepository.save(property);
        propertyId = property.getId();
    }

    @Test
    void landlordCreatesBookingRequest_tenantAccepts_landlordRequestsPayment_tenantPays_landlordConfirms() throws Exception {
        // Landlord creates direct booking request
        var bookingRes = mockMvc.perform(post("/api/dashboard/landlord/bookings/direct")
                        .with(user("landlord@test.com").roles("LANDLORD"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantEmail":"tenant@test.com","propertyId":%d,"startDate":"2026-03-23","endDate":"2027-03-31"}
                                """.formatted(propertyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Parse bookingId very simply
        long bookingId = Long.parseLong(bookingRes.replaceAll(".*\"bookingId\"\\s*:\\s*(\\d+).*", "$1"));

        // Tenant sees booking with property address in /api/bookings/my
        mockMvc.perform(get("/api/bookings/my")
                        .with(user("tenant@test.com").roles("TENANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].property.address", is("Москва Арбат 44")));

        // Tenant accepts booking
        mockMvc.perform(put("/api/bookings/%d/accept".formatted(bookingId))
                        .with(user("tenant@test.com").roles("TENANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("подтверждена")));

        // Landlord requests payment
        var payRes = mockMvc.perform(post("/api/payments/request")
                        .with(user("landlord@test.com").roles("LANDLORD"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookingId":%d,"amount":55000,"description":"Аренда за март"}
                                """.formatted(bookingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long paymentId = Long.parseLong(payRes.replaceAll(".*\"paymentId\"\\s*:\\s*(\\d+).*", "$1"));

        // Tenant marks paid
        mockMvc.perform(post("/api/payments/%d/tenant-paid".formatted(paymentId))
                        .with(user("tenant@test.com").roles("TENANT")))
                .andExpect(status().isOk());

        // Landlord sees payment status tenant_paid via DTO
        mockMvc.perform(get("/api/payments/booking/%d".formatted(bookingId))
                        .with(user("landlord@test.com").roles("LANDLORD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status", is("tenant_paid")))
                .andExpect(jsonPath("$[0].propertyAddress", is("Москва Арбат 44")))
                .andExpect(jsonPath("$[0].description", is("Аренда за март")));

        // Landlord confirms
        mockMvc.perform(post("/api/payments/%d/confirm".formatted(paymentId))
                        .with(user("landlord@test.com").roles("LANDLORD")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/payments/booking/%d".formatted(bookingId))
                        .with(user("landlord@test.com").roles("LANDLORD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status", is("confirmed")));
    }
}

