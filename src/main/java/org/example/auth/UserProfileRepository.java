package org.example.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByFirebaseUid(String firebaseUid);

    List<UserProfile> findByEmailIn(Collection<String> emails);
}
