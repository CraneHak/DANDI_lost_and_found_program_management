package org.example.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockBean
    private FirebaseAdminInitializer firebaseAdminInitializer;

    @Test
    void publicHealthEndpointShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void protectedEndpointWithoutAuthorizationHeaderShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing or invalid Authorization header."));
    }

    @Test
    void protectedEndpointWithInvalidTokenShouldReturnUnauthorized() throws Exception {
        when(firebaseTokenVerifier.verify(anyString()))
                .thenThrow(new InvalidFirebaseTokenException("Invalid Firebase ID token.", null));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid Firebase ID token."));
    }

    @Test
    void protectedEndpointWithUnverifiedEmailShouldReturnForbidden() throws Exception {
        when(firebaseTokenVerifier.verify(anyString()))
                .thenReturn(new DecodedFirebaseUser("uid-1", "abc@dankook.ac.kr", false));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Verified email is required."));
    }

    @Test
    void protectedEndpointWithNonDankookEmailShouldReturnForbidden() throws Exception {
        when(firebaseTokenVerifier.verify(anyString()))
                .thenReturn(new DecodedFirebaseUser("uid-2", "abc@gmail.com", true));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only @dankook.ac.kr email is allowed."));
    }

    @Test
    void protectedEndpointWithDankookEmailShouldReturnOk() throws Exception {
        when(firebaseTokenVerifier.verify(anyString()))
                .thenReturn(new DecodedFirebaseUser("uid-3", "abc@dankook.ac.kr", true));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("uid-3"))
                .andExpect(jsonPath("$.email").value("abc@dankook.ac.kr"));
    }
}
