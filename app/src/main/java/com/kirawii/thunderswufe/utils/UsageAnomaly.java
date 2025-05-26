package com.kirawii.thunderswufe.utils;

import java.time.LocalDateTime;

public class UsageAnomaly {
    private final AnomalyType type;
    private final LocalDateTime timestamp;
    private final double value;
    private final String description;

    public UsageAnomaly(AnomalyType type, LocalDateTime timestamp, double value, String description) {
        this.type = type;
        this.timestamp = timestamp;
        this.value = value;
        this.description = description;
    }

    public AnomalyType getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public double getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
} 