package org.example.auth;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserProfileServiceTest {
    private final UserProfileRepository repository = mock(UserProfileRepository.class);
    private final UserProfileService service = new UserProfileService(repository);

    private UserProfile existingProfile() {
        UserProfile profile = new UserProfile();
        profile.setFirebaseUid("uid");
        profile.setEmail("alias@dankook.ac.kr");
        when(repository.findByFirebaseUid("uid")).thenReturn(Optional.of(profile));
        when(repository.save(any(UserProfile.class))).thenAnswer(call -> call.getArgument(0));
        return profile;
    }

    @Test void savesAndEditsTheStudentNumberOnTheSameAccount() {
        UserProfile profile = existingProfile();
        service.updateProfile("uid", "alias@dankook.ac.kr", "Name", "Department", "20261234");
        assertEquals("20261234", profile.getStudentNo());
        assertTrue(profile.isProfileCompleted());
        service.updateProfile("uid", "alias@dankook.ac.kr", "Name", "Department", "20261235");
        assertEquals("20261235", profile.getStudentNo());
        assertEquals("uid", profile.getFirebaseUid());
        assertEquals(UserRole.ROLE_USER, profile.getRole());
    }

    @Test void emailChangesDoNotReplaceTheStoredStudentNumber() {
        UserProfile profile = existingProfile();
        profile.setStudentNo("20261234");
        service.findOrCreate("uid", "new-alias@dankook.ac.kr");
        assertEquals("20261234", profile.getStudentNo());
        assertEquals("new-alias@dankook.ac.kr", profile.getEmail());
    }

    @Test void numericEmailDoesNotCreateAStudentNumber() {
        when(repository.findByFirebaseUid("new-user")).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class))).thenAnswer(call -> call.getArgument(0));
        UserProfile profile = service.findOrCreate("new-user", "20201234@dankook.ac.kr");
        assertNull(profile.getStudentNo());
        assertFalse(profile.isProfileCompleted());
    }

    @Test void lifecycleCompletionRequiresAllThreeProfileFields() {
        UserProfile profile = new UserProfile();
        profile.setName("Name");
        profile.setDepartment("Department");
        profile.onCreate();
        assertFalse(profile.isProfileCompleted());
        profile.setStudentNo("20261234");
        profile.onUpdate();
        assertTrue(profile.isProfileCompleted());
    }
}
