package com.kirawii.thunderswufe.utils;

import java.time.LocalDateTime;
import java.util.Objects;

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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsageAnomaly that = (UsageAnomaly) o;
        return Double.compare(that.value, value) == 0 &&
                type == that.type &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, timestamp, value, description);
    }
    
    @Override
    public String toString() {
        return "UsageAnomaly{" +
                "type=" + type +
                ", timestamp=" + timestamp +
                ", value=" + value +
                ", description='" + description + '\'' +
                '}';
    }
} 