package org.example.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class FirebaseAuthenticationToken extends AbstractAuthenticationToken {
    private final String uid;
    private final String email;

    public FirebaseAuthenticationToken(String uid, String email, boolean admin) {
        super(admin ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN")) : List.of());
        this.uid = uid;
        this.email = email;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return uid;
    }

    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }
}
