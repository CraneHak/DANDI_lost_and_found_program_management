package org.example.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Component;

@Component
public class FirebaseTokenVerifierImpl implements FirebaseTokenVerifier {
    @Override
    public DecodedFirebaseUser verify(String idToken) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            return new DecodedFirebaseUser(
                    decodedToken.getUid(),
                    decodedToken.getEmail(),
                    decodedToken.isEmailVerified()
            );
        } catch (FirebaseAuthException ex) {
            throw new InvalidFirebaseTokenException("Invalid Firebase ID token.", ex);
        }
    }
}
