package com.gomech.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomech.dto.Authentication.AuthenticationDTO;
import com.gomech.dto.Authentication.RefreshTokenRequest;
import com.gomech.model.Role;
import com.gomech.model.User;
import com.gomech.repository.UserRepository;
import com.gomech.service.MfaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MfaService mfaService;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    @Test
    void loginWithoutMfaReturnsTokens() throws Exception {
        User user = new User("Admin", "admin@gomech.com", passwordEncoder.encode("password"), Role.ADMIN);
        userRepository.save(user);

        AuthenticationDTO request = new AuthenticationDTO(user.getEmail(), "password", null);

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(false))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        assertThat(node.get("email").asText()).isEqualTo(user.getEmail());
    }

    @Test
    void loginWithMfaRequiresValidCode() throws Exception {
        User user = new User("User", "user@gomech.com", passwordEncoder.encode("password"), Role.USER);
        String secret = mfaService.generateSecret();
        user.enableMfa(mfaService.encryptSecret(secret));
        userRepository.save(user);

        AuthenticationDTO request = new AuthenticationDTO(user.getEmail(), "password", null);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mfaRequired").value(true));

        String code = mfaService.generateCode(secret);
        AuthenticationDTO requestWithCode = new AuthenticationDTO(user.getEmail(), "password", code);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(false));
    }

    @Test
    void refreshTokenGeneratesNewPair() throws Exception {
        User user = new User("Admin", "refresh@gomech.com", passwordEncoder.encode("password"), Role.ADMIN);
        userRepository.save(user);

        AuthenticationDTO request = new AuthenticationDTO(user.getEmail(), "password", null);

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(loginResponse);
        String refreshToken = node.get("refreshToken").asText();

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }
}
