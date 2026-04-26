package org.example.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReportStatus {
    PENDING("pending"),
    RESOLVED("resolved"),
    PICKED_UP("picked_up"),
    UNAVAILABLE("unavailable");

    private final String value;

    ReportStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ReportStatus from(String value) {
        for (ReportStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
