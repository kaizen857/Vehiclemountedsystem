package com.example.vehicle_mountedsystem.model;

import java.util.Objects;

public final class BatteryStatus {
    private final int percent;
    private final int powerMilliwatts;
    private final AvailabilityStatus availabilityStatus;

    public BatteryStatus(int percent, int powerMilliwatts, AvailabilityStatus availabilityStatus) {
        this.percent = clamp(percent, 0, 100);
        this.powerMilliwatts = powerMilliwatts;
        this.availabilityStatus = Objects.requireNonNull(availabilityStatus, "availabilityStatus");
    }

    public static BatteryStatus defaultState() {
        return new BatteryStatus(0, 0, AvailabilityStatus.unavailable());
    }

    public int getPercent() {
        return percent;
    }

    public int getPowerMilliwatts() {
        return powerMilliwatts;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BatteryStatus)) {
            return false;
        }
        BatteryStatus that = (BatteryStatus) other;
        return percent == that.percent
                && powerMilliwatts == that.powerMilliwatts
                && availabilityStatus.equals(that.availabilityStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(percent, powerMilliwatts, availabilityStatus);
    }
}
