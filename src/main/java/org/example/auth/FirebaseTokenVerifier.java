package org.example.auth;

public interface FirebaseTokenVerifier {
    DecodedFirebaseUser verify(String idToken);
}
