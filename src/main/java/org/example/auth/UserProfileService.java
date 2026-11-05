package org.example.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserProfile findOrCreate(String uid, String email) {
        UserProfile userProfile = userProfileRepository.findByFirebaseUid(uid)
                .orElseGet(() -> createNew(uid, email));

        if (email != null && !email.equalsIgnoreCase(userProfile.getEmail())) {
            userProfile.setEmail(email.trim());
        }
        userProfile.setLastLoginAt(LocalDateTime.now());
        return userProfileRepository.save(userProfile);
    }

    @Transactional
    public UserProfile promoteToAdmin(UserProfile userProfile) {
        userProfile.setRole(UserRole.ROLE_ADMIN);
        return userProfileRepository.save(userProfile);
    }

    @Transactional
    public UserProfile updateProfile(String uid, String email, String name, String department, String studentId) {
        UserProfile userProfile = findOrCreate(uid, email);
        userProfile.setName(name != null ? name.trim() : null);
        userProfile.setDepartment(department != null ? department.trim() : null);
        userProfile.setStudentNo(studentId != null ? studentId.trim() : null);
        return userProfileRepository.save(userProfile);
    }

    private UserProfile createNew(String uid, String email) {
        UserProfile userProfile = new UserProfile();
        userProfile.setFirebaseUid(uid);
        userProfile.setEmail(email != null ? email.trim() : "");
        return userProfile;
    }
}
