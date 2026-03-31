package org.example.integration;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:rentaltestdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecuritySmokeTest {

    @Autowired MockMvc mockMvc;

    @MockBean MinioClient minioClient;

    @Test
    void protectedEndpoint_requiresAuthentication() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void adminOnlyEndpoint_forbidsNonAdmin() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        mockMvc.perform(get("/api/users")
                        .with(user("u@test.com").roles("TENANT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void landlordEndpoint_forbidsTenant() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        mockMvc.perform(get("/api/dashboard/landlord/properties")
                        .with(user("u@test.com").roles("TENANT")))
                .andExpect(status().isForbidden());
    }
}

