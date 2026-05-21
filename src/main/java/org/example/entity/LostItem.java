package org.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lost_item")
public class LostItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "post_no", unique = true)
    private Integer postNo;

    @Column(name = "report_id", unique = true)
    private Long reportId;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "found_location")
    private String foundLocation;

    @Column(name = "lost_location")
    private String lostLocation;

    @Column(name = "stored_location")
    private String storedLocation;

    /** 습득 일시 (프론트 lostAt/foundAt — 시·분 포함) */
    @Column(name = "stored_date")
    private LocalDateTime storedDate;

    @Column(name = "contact")
    private String contact;

    @Column(name = "color")
    private String color;

    @Column(name = "item_type")
    private String itemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ItemStatus status = ItemStatus.STORED;

    @Column(name = "masking_flag", nullable = false)
    private boolean maskingFlag;

    @Column(name = "vision_dominant_colors_json", columnDefinition = "TEXT")
    private String visionDominantColorsJson;

    @Column(name = "vision_extracted_text", columnDefinition = "TEXT")
    private String visionExtractedText;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ItemStatus.STORED;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getPostNo() { return postNo; }
    public void setPostNo(Integer postNo) { this.postNo = postNo; }

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getFoundLocation() { return foundLocation; }
    public void setFoundLocation(String foundLocation) { this.foundLocation = foundLocation; }

    public String getLostLocation() { return lostLocation; }
    public void setLostLocation(String lostLocation) { this.lostLocation = lostLocation; }

    public String getStoredLocation() { return storedLocation; }
    public void setStoredLocation(String storedLocation) { this.storedLocation = storedLocation; }

    public LocalDateTime getStoredDate() { return storedDate; }
    public void setStoredDate(LocalDateTime storedDate) { this.storedDate = storedDate; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }

    public boolean isMaskingFlag() { return maskingFlag; }
    public void setMaskingFlag(boolean maskingFlag) { this.maskingFlag = maskingFlag; }

    public String getVisionDominantColorsJson() { return visionDominantColorsJson; }
    public void setVisionDominantColorsJson(String visionDominantColorsJson) {
        this.visionDominantColorsJson = visionDominantColorsJson;
    }

    public String getVisionExtractedText() { return visionExtractedText; }
    public void setVisionExtractedText(String visionExtractedText) { this.visionExtractedText = visionExtractedText; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
