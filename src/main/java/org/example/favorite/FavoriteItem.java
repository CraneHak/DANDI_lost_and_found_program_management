package org.example.favorite;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "favorite_item")
public class FavoriteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lost_item_id", nullable = false)
    private Integer lostItemId;

    @Column(name = "requester_uid", nullable = false, length = 128)
    private String requesterUid;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }

    public Integer getLostItemId() { return lostItemId; }
    public void setLostItemId(Integer lostItemId) { this.lostItemId = lostItemId; }

    public String getRequesterUid() { return requesterUid; }
    public void setRequesterUid(String requesterUid) { this.requesterUid = requesterUid; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
