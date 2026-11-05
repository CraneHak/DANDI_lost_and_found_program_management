package org.example.auth;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firebase_uid", nullable = false, unique = true, length = 128)
    private String firebaseUid;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "student_no", length = 30)
    private String studentNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.ROLE_USER;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "profile_completed", nullable = false)
    private boolean profileCompleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /** 관심 키워드 알림 받기 (마이페이지 스위치). 서버가 키워드 알림을 보내기 전에 확인한다. */
    @Column(name = "keyword_alert_enabled", nullable = false)
    private boolean keywordAlertEnabled = true;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.lastLoginAt = now;
        if (this.role == null) {
            this.role = UserRole.ROLE_USER;
        }
        this.profileCompleted = isProfileCompleted();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.profileCompleted = isProfileCompleted();
    }

    private boolean isFilled(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public Long getId() {
        return id;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public boolean isKeywordAlertEnabled() {
        return keywordAlertEnabled;
    }

    public void setKeywordAlertEnabled(boolean keywordAlertEnabled) {
        this.keywordAlertEnabled = keywordAlertEnabled;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean isProfileCompleted() {
        // Older rows can have profile_completed=true without a student number.
        return isFilled(name) && isFilled(department)
                && studentNo != null && studentNo.matches("[0-9]{8}");
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
