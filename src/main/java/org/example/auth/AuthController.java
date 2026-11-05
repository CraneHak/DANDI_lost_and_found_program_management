package org.example.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserProfileService userProfileService;
    private final FirebaseProperties firebaseProperties;

    public AuthController(UserProfileService userProfileService, FirebaseProperties firebaseProperties) {
        this.userProfileService = userProfileService;
        this.firebaseProperties = firebaseProperties;
    }

    @GetMapping("/public/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/auth/login")
    public LoginResponse login(
            Authentication authentication,
            HttpServletRequest request,
            @RequestBody(required = false) LoginRequest body
    ) {
        FirebaseAuthenticationToken token = requireFirebaseToken(authentication);
        UserProfile userProfile = userProfileService.findOrCreate(token.getUid(), token.getEmail());
        // 설정 파일(FIREBASE_ADMIN_EMAILS)에 있는 관리자는 로그인할 때 DB role도 관리자로 올린다.
        // 내리는 건 자동으로 하지 않는다 — 관리자 해제는 DB role을 ROLE_USER로 바꿔서 한다.
        if (userProfile.getRole() != UserRole.ROLE_ADMIN && isConfigAdminEmail(token.getEmail())) {
            userProfile = userProfileService.promoteToAdmin(userProfile);
        }
        String accessToken = resolveAccessToken(request, body);
        boolean admin = resolveAdmin(token, userProfile);
        return toResponse(token, userProfile, accessToken, admin);
    }

    @GetMapping("/auth/me")
    public Map<String, String> me(Authentication authentication) {
        FirebaseAuthenticationToken token = (FirebaseAuthenticationToken) authentication;
        return Map.of(
                "uid", token.getUid(),
                "email", token.getEmail()
        );
    }

    @GetMapping("/users/me")
    public LoginResponse myProfile(Authentication authentication, HttpServletRequest request) {
        FirebaseAuthenticationToken token = requireFirebaseToken(authentication);
        UserProfile userProfile = userProfileService.findOrCreate(token.getUid(), token.getEmail());
        String accessToken = resolveAccessToken(request, null);
        boolean admin = resolveAdmin(token, userProfile);
        return toResponse(token, userProfile, accessToken, admin);
    }

    @PatchMapping("/users/me/profile")
    public LoginResponse updateMyProfile(
            Authentication authentication,
            HttpServletRequest request,
            @Valid @RequestBody UpdateProfileRequest requestBody
    ) {
        FirebaseAuthenticationToken token = requireFirebaseToken(authentication);
        UserProfile userProfile = userProfileService.updateProfile(
                token.getUid(),
                token.getEmail(),
                requestBody.name(),
                requestBody.department(),
                requestBody.studentId()
        );
        String accessToken = resolveAccessToken(request, null);
        boolean admin = resolveAdmin(token, userProfile);
        return toResponse(token, userProfile, accessToken, admin);
    }

    private FirebaseAuthenticationToken requireFirebaseToken(Authentication authentication) {
        if (!(authentication instanceof FirebaseAuthenticationToken token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return token;
    }

    private String resolveAccessToken(HttpServletRequest request, LoginRequest body) {
        if (body != null && body.idToken() != null && !body.idToken().isBlank()) {
            return body.idToken().trim();
        }
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }
        return "";
    }

    private boolean resolveAdmin(FirebaseAuthenticationToken token, UserProfile userProfile) {
        if (token.isAdmin()) {
            return true;
        }
        if (userProfile.getRole() == UserRole.ROLE_ADMIN) {
            return true;
        }
        return isConfigAdminEmail(token.getEmail());
    }

    private boolean isConfigAdminEmail(String email) {
        if (email == null) {
            return false;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return firebaseProperties.getAdminEmails().stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(normalized::equals);
    }

    private LoginResponse toResponse(
            FirebaseAuthenticationToken token,
            UserProfile userProfile,
            String accessToken,
            boolean admin
    ) {
        return new LoginResponse(
                accessToken,
                userProfile.isProfileCompleted(),
                admin,
                admin ? "ADMIN" : "USER",
                token.getUid(),
                token.getEmail(),
                userProfile.getName(),
                userProfile.getDepartment(),
                userProfile.getStudentNo()
        );
    }

    public record LoginRequest(String idToken) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "name is required") String name,
            @NotBlank(message = "department is required") String department,
            @NotBlank(message = "학번을 입력해 주세요.")
            @Pattern(regexp = "[0-9]{8}", message = "학번은 8자리 숫자로 입력해 주세요.") String studentId
    ) {
    }

    public record LoginResponse(
            String accessToken,
            boolean profileCompleted,
            boolean isAdmin,
            String role,
            String uid,
            String email,
            String name,
            String department,
            String studentId
    ) {
    }
}
