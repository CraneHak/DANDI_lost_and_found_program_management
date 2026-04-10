package org.example.auth;

public record DecodedFirebaseUser(String uid, String email, boolean emailVerified) {
}
