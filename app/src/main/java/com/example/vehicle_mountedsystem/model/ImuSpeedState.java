package com.example.vehicle_mountedsystem.model;

import java.util.Objects;

public final class ImuSpeedState {
    private final double speedMetersPerSecond;
    private final double accelerationG;
    private final AvailabilityStatus availabilityStatus;

    public ImuSpeedState(double speedMetersPerSecond, double accelerationG, AvailabilityStatus availabilityStatus) {
        if (!Double.isFinite(speedMetersPerSecond) || speedMetersPerSecond < 0.0d) {
            throw new IllegalArgumentException("speedMetersPerSecond must be finite and >= 0");
        }
        if (!Double.isFinite(accelerationG)) {
            throw new IllegalArgumentException("accelerationG must be finite");
        }
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.accelerationG = accelerationG;
        this.availabilityStatus = Objects.requireNonNull(availabilityStatus, "availabilityStatus");
    }

    public static ImuSpeedState defaultState() {
        return new ImuSpeedState(0.0d, 0.0d, AvailabilityStatus.unavailable());
    }

    public double getSpeedMetersPerSecond() {
        return speedMetersPerSecond;
    }

    public double getAccelerationG() {
        return accelerationG;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ImuSpeedState)) {
            return false;
        }
        ImuSpeedState that = (ImuSpeedState) other;
        return Double.compare(that.speedMetersPerSecond, speedMetersPerSecond) == 0
                && Double.compare(that.accelerationG, accelerationG) == 0
                && availabilityStatus.equals(that.availabilityStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(speedMetersPerSecond, accelerationG, availabilityStatus);
    }
}
