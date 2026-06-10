package com.example.vehicle_mountedsystem.model;

import java.util.Objects;

public final class SensorReading {
    private final String name;
    private final double x;
    private final double y;
    private final double z;
    private final String unit;
    private final AvailabilityStatus availabilityStatus;

    public SensorReading(String name, double x, double y, double z, String unit, AvailabilityStatus availabilityStatus) {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        this.name = requireText(name, "name");
        this.x = x;
        this.y = y;
        this.z = z;
        this.unit = requireText(unit, "unit");
        this.availabilityStatus = Objects.requireNonNull(availabilityStatus, "availabilityStatus");
    }

    public static SensorReading unavailable(String name, String unit) {
        return new SensorReading(name, 0.0d, 0.0d, 0.0d, unit, AvailabilityStatus.unavailable());
    }

    public String getName() {
        return name;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getUnit() {
        return unit;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static void requireFinite(double value, String fieldName) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be finite");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SensorReading)) {
            return false;
        }
        SensorReading that = (SensorReading) other;
        return Double.compare(that.x, x) == 0
                && Double.compare(that.y, y) == 0
                && Double.compare(that.z, z) == 0
                && name.equals(that.name)
                && unit.equals(that.unit)
                && availabilityStatus.equals(that.availabilityStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, x, y, z, unit, availabilityStatus);
    }
}
