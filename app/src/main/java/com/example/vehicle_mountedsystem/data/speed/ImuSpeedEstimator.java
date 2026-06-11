package com.example.vehicle_mountedsystem.data.speed;

import com.example.vehicle_mountedsystem.model.AvailabilityStatus;
import com.example.vehicle_mountedsystem.model.ImuSpeedState;

public final class ImuSpeedEstimator {
    private static final int CALIBRATION_SAMPLE_COUNT = 5;
    private static final double MAX_REASONABLE_BIAS_MPS2 = 3.0d;
    private static final double STILL_ACCELERATION_THRESHOLD_MPS2 = 0.05d;
    private static final double ZERO_SPEED_THRESHOLD_MPS = 0.2d;
    private static final int ZERO_SPEED_SAMPLE_COUNT = 5;
    private static final long MAX_SAMPLE_GAP_MILLIS = 500L;
    private static final double GRAVITY_MPS2 = 9.80665d;
    private static final double GYRO_STILL_THRESHOLD_RADS = 0.06d;
    private static final double BIAS_LEARNING_RATE = 0.001d;
    private static final String NEED_RESET_MESSAGE = "IMU 速度估算不可用/需重置";

    private double speedMps;
    private double biasMps2;
    private double calibrationSumMps2;
    private int calibrationSamples;
    private int stillSamples;
    private long lastTimestampMillis = -1L;
    private boolean calibrating;
    private boolean calibrationValid;
    private double lastGyroEnergy;
    private AvailabilityStatus availability = AvailabilityStatus.unavailable(NEED_RESET_MESSAGE, 0L);

    public void calibrate() {
        speedMps = 0.0d;
        biasMps2 = 0.0d;
        calibrationSumMps2 = 0.0d;
        calibrationSamples = 0;
        stillSamples = 0;
        lastTimestampMillis = -1L;
        calibrating = true;
        calibrationValid = false;
        availability = AvailabilityStatus.unavailable("IMU 速度估算校准中", 0L);
    }

    public void reset() {
        speedMps = 0.0d;
        stillSamples = 0;
        lastTimestampMillis = -1L;
        calibrating = false;
        availability = calibrationValid
                ? AvailabilityStatus.available("IMU 速度已重置", 0L)
                : AvailabilityStatus.unavailable(NEED_RESET_MESSAGE, 0L);
    }

    public void setGyroEnergy(double gyroEnergy) {
        this.lastGyroEnergy = gyroEnergy;
    }

    public ImuSpeedState updateSample(double linearAccelerationMps2, long timestampMillis) {
        return updateSample(linearAccelerationMps2, 0.0d, 0.0d, timestampMillis);
    }

    public ImuSpeedState updateSample(double xMps2, double yMps2, double zMps2, long timestampMillis) {
        validateSample(xMps2, yMps2, zMps2, timestampMillis);
        double accelerationMagnitudeG = Math.sqrt(xMps2 * xMps2 + yMps2 * yMps2 + zMps2 * zMps2) / GRAVITY_MPS2;
        if (calibrating) {
            return collectCalibration(xMps2, accelerationMagnitudeG, timestampMillis);
        }
        if (!availability.isAvailable()) {
            return state(accelerationMagnitudeG);
        }
        if (lastTimestampMillis < 0L) {
            lastTimestampMillis = timestampMillis;
            return state(accelerationMagnitudeG);
        }

        long deltaMillis = timestampMillis - lastTimestampMillis;
        if (deltaMillis <= 0L) {
            availability = AvailabilityStatus.unavailable("IMU 样本时间戳无效", timestampMillis);
            speedMps = 0.0d;
            return state(accelerationMagnitudeG);
        }
        if (deltaMillis > MAX_SAMPLE_GAP_MILLIS) {
            availability = AvailabilityStatus.unavailable("IMU 采样率过低，需重置", timestampMillis);
            speedMps = 0.0d;
            return state(accelerationMagnitudeG);
        }

        double correctedAcceleration = xMps2 - biasMps2;
        speedMps = safeSpeed(speedMps + correctedAcceleration * (deltaMillis / 1000.0d));
        applyZeroSpeedCorrection(correctedAcceleration);
        lastTimestampMillis = timestampMillis;
        availability = AvailabilityStatus.available("IMU 短时估算可用，存在积分漂移", timestampMillis);
        return state(accelerationMagnitudeG);
    }

    public double getSpeedMps() {
        return speedMps;
    }

    public AvailabilityStatus getAvailability() {
        return availability;
    }

    private ImuSpeedState collectCalibration(double xMps2, double accelerationMagnitudeG, long timestampMillis) {
        calibrationSumMps2 += xMps2;
        calibrationSamples++;
        speedMps = 0.0d;
        lastTimestampMillis = timestampMillis;
        if (calibrationSamples < CALIBRATION_SAMPLE_COUNT) {
            availability = AvailabilityStatus.unavailable("IMU 速度估算校准中", timestampMillis);
            return state(accelerationMagnitudeG);
        }
        biasMps2 = calibrationSumMps2 / calibrationSamples;
        calibrating = false;
        if (Math.abs(biasMps2) > MAX_REASONABLE_BIAS_MPS2) {
            calibrationValid = false;
            availability = AvailabilityStatus.unavailable("IMU 零偏过大，校准失败", timestampMillis);
            return state(accelerationMagnitudeG);
        }
        calibrationValid = true;
        availability = AvailabilityStatus.available("IMU 零偏校准完成", timestampMillis);
        return state(accelerationMagnitudeG);
    }

    private void applyZeroSpeedCorrection(double correctedAcceleration) {
        boolean accelStill = Math.abs(correctedAcceleration) <= STILL_ACCELERATION_THRESHOLD_MPS2;
        boolean gyroStill = lastGyroEnergy <= GYRO_STILL_THRESHOLD_RADS;
        if (accelStill && gyroStill) {
            stillSamples++;
            biasMps2 += BIAS_LEARNING_RATE * correctedAcceleration;
            if (Math.abs(biasMps2) > MAX_REASONABLE_BIAS_MPS2) {
                biasMps2 = Math.signum(biasMps2) * MAX_REASONABLE_BIAS_MPS2;
            }
        } else {
            stillSamples = 0;
        }
        if (stillSamples >= ZERO_SPEED_SAMPLE_COUNT && speedMps <= ZERO_SPEED_THRESHOLD_MPS) {
            speedMps = 0.0d;
        }
    }

    private ImuSpeedState state(double accelerationMagnitudeG) {
        return new ImuSpeedState(speedMps, accelerationMagnitudeG, availability);
    }

    private static void validateSample(double xMps2, double yMps2, double zMps2, long timestampMillis) {
        if (!Double.isFinite(xMps2) || !Double.isFinite(yMps2) || !Double.isFinite(zMps2)) {
            throw new IllegalArgumentException("linear acceleration must be finite");
        }
        if (timestampMillis < 0L) {
            throw new IllegalArgumentException("timestampMillis must be >= 0");
        }
    }

    private static double safeSpeed(double value) {
        if (!Double.isFinite(value) || value < 0.0d) {
            return 0.0d;
        }
        return value;
    }
}
