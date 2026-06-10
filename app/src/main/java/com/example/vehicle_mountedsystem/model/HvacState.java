package com.example.vehicle_mountedsystem.model;

import java.util.Objects;

public final class HvacState {
    public static final int MIN_TEMPERATURE_CELSIUS = 16;
    public static final int MAX_TEMPERATURE_CELSIUS = 30;
    public static final int MIN_FAN_LEVEL = 0;
    public static final int MAX_FAN_LEVEL = 5;
    public static final String DEFAULT_MODE = "AUTO";
    public static final String DEFAULT_MODE_LABEL = "自动";

    private final int temperatureCelsius;
    private final int fanLevel;
    private final String mode;
    private final String modeLabel;
    private final boolean acEnabled;
    private final boolean innerCirculationEnabled;

    public HvacState(
            int temperatureCelsius,
            int fanLevel,
            String mode,
            String modeLabel,
            boolean acEnabled,
            boolean innerCirculationEnabled) {
        this.temperatureCelsius = clamp(temperatureCelsius, MIN_TEMPERATURE_CELSIUS, MAX_TEMPERATURE_CELSIUS);
        this.fanLevel = clamp(fanLevel, MIN_FAN_LEVEL, MAX_FAN_LEVEL);
        this.mode = requireText(mode, "mode");
        this.modeLabel = requireText(modeLabel, "modeLabel");
        this.acEnabled = acEnabled;
        this.innerCirculationEnabled = innerCirculationEnabled;
    }

    public static HvacState defaultState() {
        return new HvacState(24, 2, DEFAULT_MODE, DEFAULT_MODE_LABEL, false, false);
    }

    public int getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public int getFanLevel() {
        return fanLevel;
    }

    public String getMode() {
        return mode;
    }

    public String getModeLabel() {
        return modeLabel;
    }

    public boolean isAcEnabled() {
        return acEnabled;
    }

    public boolean isInnerCirculationEnabled() {
        return innerCirculationEnabled;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof HvacState)) {
            return false;
        }
        HvacState that = (HvacState) other;
        return temperatureCelsius == that.temperatureCelsius
                && fanLevel == that.fanLevel
                && acEnabled == that.acEnabled
                && innerCirculationEnabled == that.innerCirculationEnabled
                && mode.equals(that.mode)
                && modeLabel.equals(that.modeLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(temperatureCelsius, fanLevel, mode, modeLabel, acEnabled, innerCirculationEnabled);
    }
}
