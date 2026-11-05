package org.example.adminlog;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 관리자 작업 이력 (관리자 화면 '작업 이력' 탭). 브라우저가 아니라 DB에 남겨 기기·관리자와 상관없이 보인다. */
@Entity
@Table(name = "admin_action_log")
public class AdminActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action", nullable = false, length = 30)
    private String action;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "actor_uid", length = 128)
    private String actorUid;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getActorUid() { return actorUid; }
    public void setActorUid(String actorUid) { this.actorUid = actorUid; }
    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
