package org.example.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class FirebaseAdminInitializer {
    private final FirebaseProperties firebaseProperties;

    public FirebaseAdminInitializer(FirebaseProperties firebaseProperties) {
        this.firebaseProperties = firebaseProperties;
    }

    @PostConstruct
    public void initialize() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        String adminSdkPath = firebaseProperties.getAdminSdkPath();
        if (adminSdkPath == null || adminSdkPath.isBlank()) {
            throw new IllegalStateException("FIREBASE_ADMIN_SDK_PATH is not configured.");
        }

        Path credentialPath = Path.of(adminSdkPath);
        if (!Files.exists(credentialPath)) {
            throw new IllegalStateException("Firebase admin SDK file does not exist: " + credentialPath);
        }

        try (InputStream serviceAccount = new FileInputStream(credentialPath.toFile())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
        }
    }
}
