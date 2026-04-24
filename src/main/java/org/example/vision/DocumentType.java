package org.example.vision;

public enum DocumentType {
    NONE,
    ID_CARD,
    STUDENT_ID,
    BANK_CARD;

    public boolean requiresOcr() {
        return this != NONE;
    }
}
