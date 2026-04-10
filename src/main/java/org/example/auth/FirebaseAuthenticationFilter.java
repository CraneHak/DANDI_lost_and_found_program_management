package org.example.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private final FirebaseProperties firebaseProperties;

    public FirebaseAuthenticationFilter(FirebaseProperties firebaseProperties) {
        this.firebaseProperties = firebaseProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header.");
            return;
        }

        String idToken = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (idToken.isEmpty()) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Missing Firebase ID token.");
            return;
        }

        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String email = decodedToken.getEmail();
            if (email == null || !decodedToken.isEmailVerified()) {
                writeError(response, HttpStatus.FORBIDDEN, "Verified email is required.");
                return;
            }

            String allowedDomain = firebaseProperties.getAllowedDomain();
            if (!email.toLowerCase().endsWith("@" + allowedDomain.toLowerCase())) {
                writeError(response, HttpStatus.FORBIDDEN, "Only @" + allowedDomain + " email is allowed.");
                return;
            }

            FirebaseAuthenticationToken authentication =
                    new FirebaseAuthenticationToken(decodedToken.getUid(), email);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (FirebaseAuthException ex) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Invalid Firebase ID token.");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (!PATH_MATCHER.match("/api/**", path)) {
            return true;
        }
        return PATH_MATCHER.match("/api/public/**", path);
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
