package org.example.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final UserProfileService userProfileService;

    public AuthController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/public/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/auth/login")
    public LoginResponse login(Authentication authentication) {
        FirebaseAuthenticationToken token = requireFirebaseToken(authentication);
        UserProfile userProfile = userProfileService.findOrCreate(token.getUid(), token.getEmail());
        return toResponse(token, userProfile);
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
    public LoginResponse myProfile(Authentication authentication) {
        FirebaseAuthenticationToken token = requireFirebaseToken(authentication);
        UserProfile userProfile = userProfileService.findOrCreate(token.getUid(), token.getEmail());
        return toResponse(token, userProfile);
    }

    @PatchMapping("/users/me/profile")
    public LoginResponse updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        FirebaseAuthenticationToken token = requireFirebaseToken(authentication);
        UserProfile userProfile = userProfileService.updateProfile(
                token.getUid(),
                token.getEmail(),
                request.name(),
                request.department()
        );
        return toResponse(token, userProfile);
    }

    private FirebaseAuthenticationToken requireFirebaseToken(Authentication authentication) {
        if (!(authentication instanceof FirebaseAuthenticationToken token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return token;
    }

    private LoginResponse toResponse(FirebaseAuthenticationToken token, UserProfile userProfile) {
        return new LoginResponse(
                token.getUid(),
                token.getEmail(),
                userProfile.getName(),
                userProfile.getDepartment(),
                userProfile.isProfileCompleted()
        );
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "name is required") String name,
            @NotBlank(message = "department is required") String department
    ) {
    }

    public record LoginResponse(
            String uid,
            String email,
            String name,
            String department,
            boolean profileCompleted
    ) {
    }
}
