package com.example.vehicle_mountedsystem.data.speed;

import android.hardware.SensorManager;

import com.example.vehicle_mountedsystem.data.sensor.MotionSensorProvider;
import com.example.vehicle_mountedsystem.model.AvailabilityStatus;
import com.example.vehicle_mountedsystem.model.ImuSpeedState;

public final class ImuSpeedEstimator implements MotionSensorProvider.SourceListener {
    private static final int WINDOW_SIZE = 16;
    private static final double GRAVITY_MPS2 = 9.80665d;
    private static final double VAR_ACCEL_THRESHOLD = 0.004d;
    private static final double GYRO_ENERGY_THRESHOLD = 0.06d;
    private static final int REQUIRED_STILL_SAMPLES = 6;
    private static final double MAX_REASONABLE_BIAS_MPS2 = 3.0d;
    private static final long MAX_SAMPLE_GAP_MILLIS = 500L;

    private double speedMps = 0.0d;
    private double biasMps2 = 0.0d;
    private long lastAccelTimestampMillis = -1L;

    private final double[] accelXWindow = new double[WINDOW_SIZE];
    private final double[] accelYWindow = new double[WINDOW_SIZE];
    private final double[] accelZWindow = new double[WINDOW_SIZE];
    private final double[] gyroEnergyWindow = new double[WINDOW_SIZE];
    private int windowIndex = 0;
    private int samplesCollected = 0;
    private int stillSamples = 0;

    private final float[] rotVec = new float[4];
    private final float[] rotMatrix = new float[9];
    private final float[] remappedMatrix = new float[9];
    private boolean hasRotation = false;

    private double latestGyroEnergy = 0.0d;

    private boolean calibrating = false;
    private int calibrationSamples = 0;
    private double calibrationSumMps2 = 0.0d;
    private boolean calibrationValid = false;

    private double currentGValue = 0.0d;

    private AvailabilityStatus availability = AvailabilityStatus.unavailable("等待传感器初始化", 0L);

    public synchronized void calibrate() {
        speedMps = 0.0d;
        biasMps2 = 0.0d;
        calibrationSumMps2 = 0.0d;
        calibrationSamples = 0;
        stillSamples = 0;
        calibrating = true;
        calibrationValid = false;
        samplesCollected = 0;
        windowIndex = 0;
        lastAccelTimestampMillis = -1L;
        availability = AvailabilityStatus.unavailable("车速估算系统校准中", System.currentTimeMillis());
    }

    public synchronized void reset() {
        speedMps = 0.0d;
        stillSamples = 0;
        calibrating = false;
        lastAccelTimestampMillis = -1L;
        availability = calibrationValid
                ? AvailabilityStatus.available("车速估算已重设", System.currentTimeMillis())
                : AvailabilityStatus.unavailable("车速估算不可用", System.currentTimeMillis());
    }

    public synchronized ImuSpeedState getCurrentState() {
        return new ImuSpeedState(speedMps, currentGValue, availability);
    }

    @Override
    public synchronized void onSensorSample(MotionSensorProvider.SensorSample sample) {
        int type = sample.getSensorType();
        long ts = sample.getTimestampMillis();

        if (type == MotionSensorProvider.SENSOR_GAME_ROTATION_VECTOR ||
                type == MotionSensorProvider.SENSOR_ROTATION_VECTOR) {
            float rx = (float) sample.getX();
            float ry = (float) sample.getY();
            float rz = (float) sample.getZ();
            float rw = (float) Math.sqrt(Math.max(0.0, 1.0 - rx * rx - ry * ry - rz * rz));
            rotVec[0] = rx;
            rotVec[1] = ry;
            rotVec[2] = rz;
            rotVec[3] = rw;
            SensorManager.getRotationMatrixFromVector(rotMatrix, rotVec);
            SensorManager.remapCoordinateSystem(rotMatrix, SensorManager.AXIS_Z, SensorManager.AXIS_X, remappedMatrix);
            hasRotation = true;
        } else if (type == MotionSensorProvider.SENSOR_GYROSCOPE) {
            double gx = sample.getX();
            double gy = sample.getY();
            double gz = sample.getZ();
            latestGyroEnergy = Math.sqrt(gx * gx + gy * gy + gz * gz);
        } else if (type == MotionSensorProvider.SENSOR_ACCELEROMETER) {
            currentGValue = Math.sqrt(sample.getX() * sample.getX() + sample.getY() * sample.getY() + sample.getZ() * sample.getZ()) / GRAVITY_MPS2;
        } else if (type == MotionSensorProvider.SENSOR_LINEAR_ACCELERATION) {
            if (!hasRotation) {
                availability = AvailabilityStatus.unavailable("等待姿态数据对齐", ts);
                return;
            }

            double dx = sample.getX();
            double dy = sample.getY();
            double dz = sample.getZ();

            // Rotate device frame acceleration to world frame using remapped matrix
            double fwd = remappedMatrix[0] * dx + remappedMatrix[1] * dy + remappedMatrix[2] * dz;

            // Slide window for variance check
            accelXWindow[windowIndex] = dx;
            accelYWindow[windowIndex] = dy;
            accelZWindow[windowIndex] = dz;
            gyroEnergyWindow[windowIndex] = latestGyroEnergy;

            windowIndex = (windowIndex + 1) % WINDOW_SIZE;
            if (samplesCollected < WINDOW_SIZE) samplesCollected++;

            processLinearAcceleration(fwd, ts);
        }
    }

    private void processLinearAcceleration(double worldForwardAccel, long ts) {
        if (calibrating) {
            calibrationSumMps2 += worldForwardAccel;
            calibrationSamples++;
            if (calibrationSamples >= 5) {
                biasMps2 = calibrationSumMps2 / calibrationSamples;
                calibrating = false;
                calibrationValid = Math.abs(biasMps2) <= MAX_REASONABLE_BIAS_MPS2;
                availability = calibrationValid ?
                        AvailabilityStatus.available("车速校准完成", ts) :
                        AvailabilityStatus.unavailable("车速校准失败：环境波动过大", ts);
            } else {
                availability = AvailabilityStatus.unavailable("车速估算系统校准中", ts);
            }
            lastAccelTimestampMillis = ts;
            return;
        }

        if (!calibrationValid) {
            return;
        }

        if (lastAccelTimestampMillis < 0) {
            lastAccelTimestampMillis = ts;
            return;
        }

        long dtMillis = ts - lastAccelTimestampMillis;
        if (dtMillis <= 0 || dtMillis > MAX_SAMPLE_GAP_MILLIS) {
            if (dtMillis > MAX_SAMPLE_GAP_MILLIS) {
                availability = AvailabilityStatus.unavailable("传感器采样异常", ts);
                speedMps = 0.0d;
            }
            lastAccelTimestampMillis = ts;
            return;
        }

        double dtSeconds = dtMillis / 1000.0d;
        lastAccelTimestampMillis = ts;

        boolean isStill = checkZUPT();

        if (isStill) {
            stillSamples++;
            // Exponential moving average to update bias over time
            biasMps2 = 0.999 * biasMps2 + 0.001 * worldForwardAccel;

            if (stillSamples >= REQUIRED_STILL_SAMPLES) {
                if (speedMps < 0.2d) {
                    speedMps = 0.0d; // Hard stop
                } else {
                    speedMps *= 0.9d; // Soft pull to zero to prevent sudden UI jumps
                }
            }
        } else {
            stillSamples = 0;
            double correctedAccel = worldForwardAccel - biasMps2;
            speedMps += correctedAccel * dtSeconds;
            if (speedMps < 0.0d) speedMps = 0.0d;
        }

        availability = AvailabilityStatus.available("车速估算已就绪", ts);
    }

    private boolean checkZUPT() {
        if (samplesCollected < WINDOW_SIZE) return false;

        double sumX = 0, sumY = 0, sumZ = 0, sumGyro = 0;
        for (int i = 0; i < WINDOW_SIZE; i++) {
            sumX += accelXWindow[i];
            sumY += accelYWindow[i];
            sumZ += accelZWindow[i];
            sumGyro += gyroEnergyWindow[i];
        }

        double meanGyro = sumGyro / WINDOW_SIZE;
        if (meanGyro > GYRO_ENERGY_THRESHOLD) {
            return false;
        }

        double meanX = sumX / WINDOW_SIZE;
        double meanY = sumY / WINDOW_SIZE;
        double meanZ = sumZ / WINDOW_SIZE;

        double varSum = 0;
        for (int i = 0; i < WINDOW_SIZE; i++) {
            double dx = accelXWindow[i] - meanX;
            double dy = accelYWindow[i] - meanY;
            double dz = accelZWindow[i] - meanZ;
            varSum += (dx * dx + dy * dy + dz * dz);
        }

        double variance = varSum / WINDOW_SIZE;
        return variance <= VAR_ACCEL_THRESHOLD;
    }

    // --- Backwards Compatibility for Tests ---

    public synchronized AvailabilityStatus getAvailability() {
        return availability;
    }

    public synchronized double getSpeedMps() {
        return speedMps;
    }

    public synchronized void setGyroEnergy(double gyroEnergy) {
        this.latestGyroEnergy = gyroEnergy;
    }

    public synchronized ImuSpeedState updateSample(double xMps2, long timestampMillis) {
        return updateSample(xMps2, 0.0d, 0.0d, timestampMillis);
    }

    public synchronized ImuSpeedState updateSample(double xMps2, double yMps2, double zMps2, long timestampMillis) {
        hasRotation = true;
        // Mock identity rotation so fwd = xMps2
        remappedMatrix[0] = 1; remappedMatrix[1] = 0; remappedMatrix[2] = 0;
        MotionSensorProvider.SensorSample sample = new MotionSensorProvider.SensorSample(
                MotionSensorProvider.SENSOR_LINEAR_ACCELERATION, xMps2, yMps2, zMps2, timestampMillis);
        onSensorSample(sample);
        return getCurrentState();
    }
}