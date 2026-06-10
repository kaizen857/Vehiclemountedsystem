package com.example.vehicle_mountedsystem.model;

import java.util.Objects;

public final class AvailabilityStatus {
    public static final long DEFAULT_TIMESTAMP_MILLIS = 0L;
    public static final String DEFAULT_AVAILABLE_MESSAGE = "可用";
    public static final String DEFAULT_UNAVAILABLE_MESSAGE = "不可用";

    private final boolean available;
    private final String message;
    private final long timestampMillis;

    private AvailabilityStatus(boolean available, String message, long timestampMillis) {
        if (timestampMillis < 0L) {
            throw new IllegalArgumentException("timestampMillis must be >= 0");
        }
        this.available = available;
        this.message = requireMessage(message);
        this.timestampMillis = timestampMillis;
    }

    public static AvailabilityStatus available() {
        return available(DEFAULT_AVAILABLE_MESSAGE, DEFAULT_TIMESTAMP_MILLIS);
    }

    public static AvailabilityStatus available(String message, long timestampMillis) {
        return new AvailabilityStatus(true, message, timestampMillis);
    }

    public static AvailabilityStatus unavailable() {
        return unavailable(DEFAULT_UNAVAILABLE_MESSAGE, DEFAULT_TIMESTAMP_MILLIS);
    }

    public static AvailabilityStatus unavailable(String message, long timestampMillis) {
        return new AvailabilityStatus(false, message, timestampMillis);
    }

    public boolean isAvailable() {
        return available;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    private static String requireMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        return message;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AvailabilityStatus)) {
            return false;
        }
        AvailabilityStatus that = (AvailabilityStatus) other;
        return available == that.available
                && timestampMillis == that.timestampMillis
                && message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(available, message, timestampMillis);
    }
}
