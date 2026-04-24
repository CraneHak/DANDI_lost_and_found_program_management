package org.example.vision;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "lost_item_analysis")
public class LostItemAnalysisRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private boolean ocrApplied;

    @Column(nullable = false)
    private String originalImagePath;

    private String mosaicImagePath;

    @Lob
    private String maskedText;

    @Lob
    private String objectLabelsJson;

    @Lob
    private String dominantColorsJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public boolean isOcrApplied() {
        return ocrApplied;
    }

    public void setOcrApplied(boolean ocrApplied) {
        this.ocrApplied = ocrApplied;
    }

    public String getOriginalImagePath() {
        return originalImagePath;
    }

    public void setOriginalImagePath(String originalImagePath) {
        this.originalImagePath = originalImagePath;
    }

    public String getMosaicImagePath() {
        return mosaicImagePath;
    }

    public void setMosaicImagePath(String mosaicImagePath) {
        this.mosaicImagePath = mosaicImagePath;
    }

    public String getMaskedText() {
        return maskedText;
    }

    public void setMaskedText(String maskedText) {
        this.maskedText = maskedText;
    }

    public String getObjectLabelsJson() {
        return objectLabelsJson;
    }

    public void setObjectLabelsJson(String objectLabelsJson) {
        this.objectLabelsJson = objectLabelsJson;
    }

    public String getDominantColorsJson() {
        return dominantColorsJson;
    }

    public void setDominantColorsJson(String dominantColorsJson) {
        this.dominantColorsJson = dominantColorsJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
