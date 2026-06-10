package com.example.vehicle_mountedsystem.model;

import java.util.Objects;

public final class VehicleState {
    private final double speedMetersPerSecond;
    private final GearState gearState;
    private final HvacState hvacState;
    private final BatteryStatus batteryStatus;
    private final ImuSpeedState imuSpeedState;
    private final MediaState mediaState;

    public VehicleState(
            double speedMetersPerSecond,
            GearState gearState,
            HvacState hvacState,
            BatteryStatus batteryStatus,
            ImuSpeedState imuSpeedState,
            MediaState mediaState) {
        if (!Double.isFinite(speedMetersPerSecond) || speedMetersPerSecond < 0.0d) {
            throw new IllegalArgumentException("speedMetersPerSecond must be finite and >= 0");
        }
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.gearState = Objects.requireNonNull(gearState, "gearState");
        this.hvacState = Objects.requireNonNull(hvacState, "hvacState");
        this.batteryStatus = Objects.requireNonNull(batteryStatus, "batteryStatus");
        this.imuSpeedState = Objects.requireNonNull(imuSpeedState, "imuSpeedState");
        this.mediaState = Objects.requireNonNull(mediaState, "mediaState");
    }

    public static VehicleState defaultState() {
        return new VehicleState(
                0.0d,
                GearState.defaultState(),
                HvacState.defaultState(),
                BatteryStatus.defaultState(),
                ImuSpeedState.defaultState(),
                MediaState.defaultState());
    }

    public double getSpeedMetersPerSecond() {
        return speedMetersPerSecond;
    }

    public GearState getGearState() {
        return gearState;
    }

    public HvacState getHvacState() {
        return hvacState;
    }

    public BatteryStatus getBatteryStatus() {
        return batteryStatus;
    }

    public ImuSpeedState getImuSpeedState() {
        return imuSpeedState;
    }

    public MediaState getMediaState() {
        return mediaState;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VehicleState)) {
            return false;
        }
        VehicleState that = (VehicleState) other;
        return Double.compare(that.speedMetersPerSecond, speedMetersPerSecond) == 0
                && gearState == that.gearState
                && hvacState.equals(that.hvacState)
                && batteryStatus.equals(that.batteryStatus)
                && imuSpeedState.equals(that.imuSpeedState)
                && mediaState.equals(that.mediaState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(speedMetersPerSecond, gearState, hvacState, batteryStatus, imuSpeedState, mediaState);
    }
}
