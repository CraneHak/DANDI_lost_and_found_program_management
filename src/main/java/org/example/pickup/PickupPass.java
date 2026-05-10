package org.example.pickup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.example.entity.LostItem;

import java.time.OffsetDateTime;

@Entity
@Table(name = "pickup_pass")
public class PickupPass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 120)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lost_item_id", nullable = false)
    private LostItem lostItem;

    @Column(name = "requester_uid", nullable = false, length = 128)
    private String requesterUid;

    @Column(name = "requester_email", nullable = false, length = 255)
    private String requesterEmail;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "verified_by_uid", length = 128)
    private String verifiedByUid;

    @Column(name = "verified_by_email", length = 255)
    private String verifiedByEmail;

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LostItem getLostItem() {
        return lostItem;
    }

    public void setLostItem(LostItem lostItem) {
        this.lostItem = lostItem;
    }

    public String getRequesterUid() {
        return requesterUid;
    }

    public void setRequesterUid(String requesterUid) {
        this.requesterUid = requesterUid;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public OffsetDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(OffsetDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(OffsetDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public String getVerifiedByUid() {
        return verifiedByUid;
    }

    public void setVerifiedByUid(String verifiedByUid) {
        this.verifiedByUid = verifiedByUid;
    }

    public String getVerifiedByEmail() {
        return verifiedByEmail;
    }

    public void setVerifiedByEmail(String verifiedByEmail) {
        this.verifiedByEmail = verifiedByEmail;
    }
}
