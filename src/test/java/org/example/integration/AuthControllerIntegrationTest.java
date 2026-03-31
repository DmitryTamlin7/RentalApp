package org.example.integration;

import org.example.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;

    @Test
    void register_allowsTenantAndLandlord_only() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"t1@test.com","password":"Pass123","fullName":"Tenant One","role":"TENANT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("t1@test.com")))
                .andExpect(jsonPath("$.role", is("TENANT")));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a1@test.com","password":"Pass123","fullName":"Admin One","role":"ADMIN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Only TENANT or LANDLORD")));
    }

    @Test
    void register_duplicate_returns409() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"dup@test.com","password":"Pass123","fullName":"X","role":"TENANT"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"dup@test.com","password":"Pass123","fullName":"X","role":"TENANT"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("already exists")));
    }

    @Test
    void login_returnsJwtResponseStructure_afterRegister() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"login@test.com","password":"Pass123","fullName":"Login User","role":"TENANT"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"login@test.com","password":"Pass123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.refreshToken", not(emptyString())))
                .andExpect(jsonPath("$.email", is("login@test.com")))
                .andExpect(jsonPath("$.role", is("TENANT")));
    }
}

