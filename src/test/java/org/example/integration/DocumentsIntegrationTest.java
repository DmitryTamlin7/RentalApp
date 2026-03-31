package org.example.integration;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.ObjectWriteResponse;
import org.example.model.Booking;
import org.example.model.Property;
import org.example.repository.BookingRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
class DocumentsIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PropertyRepository propertyRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @MockBean MinioClient minioClient;

    private Long landlordId;
    private Long tenantId;
    private Long bookingId;

    @BeforeEach
    void init() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));

        byte[] bytes = "pdf".getBytes();
        ByteArrayInputStream delegate = new ByteArrayInputStream(bytes);
        GetObjectResponse response = mock(GetObjectResponse.class);
        when(response.read()).thenAnswer(inv -> delegate.read());
        when(response.read(any(byte[].class), anyInt(), anyInt())).thenAnswer(inv -> {
            byte[] b = inv.getArgument(0);
            int off = inv.getArgument(1);
            int len = inv.getArgument(2);
            return delegate.read(b, off, len);
        });
        doAnswer(inv -> {
            delegate.close();
            return null;
        }).when(response).close();

        when(minioClient.getObject(any())).thenReturn(response);

        var landlord = TestUsers.ensureUser(userRepository, passwordEncoder, "land2@test.com", "Pass123", "L2", "LANDLORD");
        var tenant = TestUsers.ensureUser(userRepository, passwordEncoder, "ten2@test.com", "Pass123", "T2", "TENANT");
        landlordId = landlord.getId();
        tenantId = tenant.getId();

        Property property = Property.builder()
                .owner(landlord)
                .address("Doc street 1")
                .description("Doc apt")
                .pricePerMonth(1000)
                .build();
        property = propertyRepository.save(property);

        Booking booking = Booking.builder()
                .tenant(tenant)
                .property(property)
                .startDate(LocalDate.parse("2026-01-01"))
                .endDate(LocalDate.parse("2026-02-01"))
                .monthlyRent(1000)
                .status("confirmed")
                .build();
        booking = bookingRepository.save(booking);
        bookingId = booking.getId();
    }

    @Test
    void upload_list_filter_and_download() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "contract.pdf",
                "application/pdf",
                "%PDF-1.4".getBytes()
        );

        // Upload by landlord for booking
        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("title", "Signed contract")
                        .param("documentType", "CONTRACT")
                        .param("bookingId", String.valueOf(bookingId))
                        .with(user("land2@test.com").roles("LANDLORD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Signed contract")))
                .andExpect(jsonPath("$.documentType", is("CONTRACT")))
                .andExpect(jsonPath("$.bookingId", is(bookingId.intValue())));

        // List visible for tenant, with filter
        mockMvc.perform(get("/api/documents")
                        .param("q", "Signed")
                        .param("documentType", "CONTRACT")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user("ten2@test.com").roles("TENANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].originalFileName", is("contract.pdf")));

        // Download first doc id from list (simple parse)
        String listJson = mockMvc.perform(get("/api/documents")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user("ten2@test.com").roles("TENANT")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long docId = Long.parseLong(listJson.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));

        mockMvc.perform(get("/api/documents/%d/download".formatted(docId))
                        .with(user("ten2@test.com").roles("TENANT")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")));
    }

    @Test
    void upload_rejectsUnsupportedMime() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bad.txt",
                "text/plain",
                "x".getBytes()
        );

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("title", "bad")
                        .param("documentType", "CONTRACT")
                        .param("bookingId", String.valueOf(bookingId))
                        .with(user("land2@test.com").roles("LANDLORD")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Неподдерживаемый тип")));
    }
}

