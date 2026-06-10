package com.example.vehicle_mountedsystem.util;

import com.example.vehicle_mountedsystem.model.AvailabilityStatus;

import java.util.Locale;

public final class UnitFormatter {
    public static final String UNAVAILABLE_LABEL = "--";

    private UnitFormatter() {
    }

    public static String formatSpeedMetersPerSecond(double speedMetersPerSecond) {
        if (!Double.isFinite(speedMetersPerSecond)) {
            return UNAVAILABLE_LABEL;
        }
        return String.format(Locale.US, "%.1f m/s", Math.max(0.0d, speedMetersPerSecond));
    }

    public static String formatBatteryPercent(int percent) {
        return String.format(Locale.US, "%d%%", clamp(percent, 0, 100));
    }

    public static String formatMilliwatts(int milliwatts) {
        return String.format(Locale.US, "%d mW", milliwatts);
    }

    public static String formatGValue(double gValue) {
        if (!Double.isFinite(gValue)) {
            return UNAVAILABLE_LABEL;
        }
        return String.format(Locale.US, "%.2f G", gValue);
    }

    public static String formatUnavailable(AvailabilityStatus availabilityStatus) {
        if (availabilityStatus == null || availabilityStatus.isAvailable()) {
            return UNAVAILABLE_LABEL;
        }
        return availabilityStatus.getMessage();
    }

    public static String formatAvailabilityAware(String formattedValue, AvailabilityStatus availabilityStatus) {
        if (availabilityStatus == null || !availabilityStatus.isAvailable()) {
            return formatUnavailable(availabilityStatus);
        }
        return formattedValue;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
