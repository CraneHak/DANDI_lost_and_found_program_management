package org.example.auth;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    @GetMapping("/public/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/auth/me")
    public Map<String, String> me(Authentication authentication) {
        FirebaseAuthenticationToken token = (FirebaseAuthenticationToken) authentication;
        return Map.of(
                "uid", token.getUid(),
                "email", token.getEmail()
        );
    }
}
