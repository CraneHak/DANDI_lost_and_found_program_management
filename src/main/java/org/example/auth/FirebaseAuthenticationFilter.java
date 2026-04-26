package org.example.auth;
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
import java.util.Locale;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private final FirebaseProperties firebaseProperties;
    private final FirebaseTokenVerifier firebaseTokenVerifier;

    public FirebaseAuthenticationFilter(
            FirebaseProperties firebaseProperties,
            FirebaseTokenVerifier firebaseTokenVerifier
    ) {
        this.firebaseProperties = firebaseProperties;
        this.firebaseTokenVerifier = firebaseTokenVerifier;
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
            DecodedFirebaseUser decodedUser = firebaseTokenVerifier.verify(idToken);
            String email = decodedUser.email();
            if (email == null || !decodedUser.emailVerified()) {
                writeError(response, HttpStatus.FORBIDDEN, "Verified email is required.");
                return;
            }

            String allowedDomain = firebaseProperties.getAllowedDomain();
            if (!email.toLowerCase().endsWith("@" + allowedDomain.toLowerCase())) {
                writeError(response, HttpStatus.FORBIDDEN, "Only @" + allowedDomain + " email is allowed.");
                return;
            }

            boolean admin = isAdmin(decodedUser.uid(), email);
            FirebaseAuthenticationToken authentication =
                    new FirebaseAuthenticationToken(decodedUser.uid(), email, admin);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (InvalidFirebaseTokenException ex) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Invalid Firebase ID token.");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!PATH_MATCHER.match("/api/**", path)) {
            return true;
        }
        return PATH_MATCHER.match("/api/public/**", path)
<<<<<<< HEAD
                || PATH_MATCHER.match("/api/reports/**", path);
=======
                || PATH_MATCHER.match("/api/notices/**", path)
                || PATH_MATCHER.match("/api/users/keywords/**", path);
>>>>>>> 5c979f8 (feat: implement /api/notices and /api/users/keywords endpoints)
    }

    private boolean isAdmin(String uid, String email) {
        if (uid != null) {
            for (String adminUid : firebaseProperties.getAdminUids()) {
                if (uid.equals(adminUid != null ? adminUid.trim() : null)) {
                    return true;
                }
            }
        }
        if (email != null) {
            String normalized = email.toLowerCase(Locale.ROOT);
            for (String adminEmail : firebaseProperties.getAdminEmails()) {
                if (adminEmail != null && normalized.equals(adminEmail.trim().toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
