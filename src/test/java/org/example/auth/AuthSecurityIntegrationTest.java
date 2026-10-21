package org.example.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.example.service.LostItemService;
import org.example.service.S3Service;
import org.springframework.test.web.servlet.MockMvc;
import org.example.vision.VisionAnalyzeService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @MockBean
    private UserProfileService userProfileService;

    // JPA auto-configuration is disabled for this test; repositories still need mock beans.
    @MockBean private UserProfileRepository userProfileRepository;
    @MockBean private org.example.repository.LostItemRepository lostItemRepository;
    @MockBean private org.example.repository.CategoryRepository categoryRepository;
    @MockBean private org.example.favorite.FavoriteItemRepository favoriteItemRepository;
    @MockBean private org.example.keyword.KeywordRepository keywordRepository;
    @MockBean private org.example.report.ReportRepository reportRepository;
    @MockBean private org.example.pickup.CollectionLogRepository collectionLogRepository;
    @MockBean private org.example.pickup.PickupPassRepository pickupPassRepository;
    @MockBean private org.example.notice.NoticeRepository noticeRepository;
    @MockBean private org.example.vision.LostItemAnalysisRepository lostItemAnalysisRepository;

    @MockBean
    private LostItemService lostItemService;

    @MockBean
    private VisionAnalyzeService visionAnalyzeService;

    @MockBean
    private S3Service s3Service;

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

    @Test
    void loginEndpointShouldProvisionProfileAndReturnCompletionFlag() throws Exception {
        when(firebaseTokenVerifier.verify(anyString()))
                .thenReturn(new DecodedFirebaseUser("uid-9", "user@dankook.ac.kr", true));

        UserProfile userProfile = new UserProfile();
        userProfile.setFirebaseUid("uid-9");
        userProfile.setEmail("user@dankook.ac.kr");
        when(userProfileService.findOrCreate("uid-9", "user@dankook.ac.kr"))
                .thenReturn(userProfile);

        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("valid-token"))
                .andExpect(jsonPath("$.uid").value("uid-9"))
                .andExpect(jsonPath("$.email").value("user@dankook.ac.kr"))
                .andExpect(jsonPath("$.profileCompleted").value(false))
                .andExpect(jsonPath("$.isAdmin").value(false));
    }

    @Test
    void updateProfileEndpointShouldReturnUpdatedProfileFields() throws Exception {
        when(firebaseTokenVerifier.verify(anyString()))
                .thenReturn(new DecodedFirebaseUser("uid-10", "user2@dankook.ac.kr", true));

        UserProfile userProfile = new UserProfile();
        userProfile.setFirebaseUid("uid-10");
        userProfile.setEmail("user2@dankook.ac.kr");
        userProfile.setName("홍길동");
        userProfile.setDepartment("컴퓨터공학과");
        userProfile.setStudentNo("20261234");
        when(userProfileService.updateProfile("uid-10", "user2@dankook.ac.kr", "홍길동", "컴퓨터공학과", "20261234"))
                .thenReturn(userProfile);

        mockMvc.perform(patch("/api/users/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"홍길동\",\"department\":\"컴퓨터공학과\",\"studentId\":\"20261234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("uid-10"))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.department").value("컴퓨터공학과"))
                .andExpect(jsonPath("$.studentId").value("20261234"))
                .andExpect(jsonPath("$.profileCompleted").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abcdefgh", "1234567", "123456789", "12ab5678"})
    void invalidStudentNumberIsRejected(String studentId) throws Exception {
        when(firebaseTokenVerifier.verify(anyString()))
                .thenReturn(new DecodedFirebaseUser("uid-validation", "alias@dankook.ac.kr", true));
        mockMvc.perform(patch("/api/users/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Name\",\"department\":\"Department\",\"studentId\":\"" + studentId + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
        verify(userProfileService, never()).updateProfile(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void missingStudentNumberIsRejected() throws Exception {
        when(firebaseTokenVerifier.verify(anyString()))
                .thenReturn(new DecodedFirebaseUser("uid-validation", "alias@dankook.ac.kr", true));
        mockMvc.perform(patch("/api/users/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Name\",\"department\":\"Department\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsStoredStudentNumberRatherThanEmailPrefix() throws Exception {
        when(firebaseTokenVerifier.verify(anyString()))
                .thenReturn(new DecodedFirebaseUser("uid-existing", "nickname@dankook.ac.kr", true));
        UserProfile profile = new UserProfile();
        profile.setName("Name");
        profile.setDepartment("Department");
        profile.setStudentNo("20261234");
        when(userProfileService.findOrCreate("uid-existing", "nickname@dankook.ac.kr")).thenReturn(profile);
        mockMvc.perform(post("/api/auth/login").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("20261234"))
                .andExpect(jsonPath("$.profileCompleted").value(true));
    }

    @Test
    void legacyProfileWithoutStudentNumberNeedsOnboarding() throws Exception {
        when(firebaseTokenVerifier.verify(anyString()))
                .thenReturn(new DecodedFirebaseUser("uid-existing", "20209999@dankook.ac.kr", true));
        UserProfile profile = new UserProfile();
        profile.setName("Name");
        profile.setDepartment("Department");
        org.springframework.test.util.ReflectionTestUtils.setField(profile, "profileCompleted", true);
        when(userProfileService.findOrCreate("uid-existing", "20209999@dankook.ac.kr")).thenReturn(profile);
        mockMvc.perform(post("/api/auth/login").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").isEmpty())
                .andExpect(jsonPath("$.profileCompleted").value(false));
    }
}
