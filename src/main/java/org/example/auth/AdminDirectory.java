package org.example.auth;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 알림 수신자 판단용 관리자 목록.
 * 기준은 FirebaseAuthenticationFilter와 같다: 설정 파일의 관리자 이메일·UID, 또는 DB role이 ROLE_ADMIN.
 */
@Component
public class AdminDirectory {
    private final UserProfileRepository userProfileRepository;
    private final FirebaseProperties firebaseProperties;

    public AdminDirectory(UserProfileRepository userProfileRepository, FirebaseProperties firebaseProperties) {
        this.userProfileRepository = userProfileRepository;
        this.firebaseProperties = firebaseProperties;
    }

    public boolean isAdminUid(String uid) {
        if (uid == null || uid.isBlank()) {
            return false;
        }
        if (firebaseProperties.getAdminUids().contains(uid)) {
            return true;
        }
        return userProfileRepository.findByFirebaseUid(uid)
                .map(this::isAdminProfile)
                .orElse(false);
    }

    /** 알림을 받을 수 있는 관리자 UID (로그인한 적 없는 관리자는 UID가 없어 빠진다) */
    public Set<String> adminUids() {
        Set<String> uids = new LinkedHashSet<>(firebaseProperties.getAdminUids());
        // ponytail: 사용자 수가 적어 전체 조회 후 거른다. 사용자가 많아지면 role/email 조건 조회로.
        for (UserProfile profile : userProfileRepository.findAll()) {
            if (isAdminProfile(profile)) {
                uids.add(profile.getFirebaseUid());
            }
        }
        uids.removeIf(uid -> uid == null || uid.isBlank());
        return uids;
    }

    private boolean isAdminProfile(UserProfile profile) {
        if (profile.getRole() == UserRole.ROLE_ADMIN) {
            return true;
        }
        String email = profile.getEmail();
        if (email == null || email.isBlank()) {
            return false;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        List<String> adminEmails = firebaseProperties.getAdminEmails();
        return adminEmails.stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(normalized::equals);
    }
}
