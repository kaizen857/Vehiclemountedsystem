package com.example.vehicle_mountedsystem;

import com.example.vehicle_mountedsystem.data.speed.ImuSpeedEstimator;
import com.example.vehicle_mountedsystem.model.ImuSpeedState;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ImuSpeedEstimatorTest {
    @Test
    public void updateSample_integratesCalibratedLinearAccelerationInMetersPerSecond() {
        ImuSpeedEstimator estimator = new ImuSpeedEstimator();

        calibrateAtRest(estimator);
        estimator.updateSample(1.0d, 900L);
        estimator.updateSample(1.0d, 1300L);
        estimator.updateSample(1.0d, 1700L);
        estimator.updateSample(1.0d, 2100L);
        ImuSpeedState state = estimator.updateSample(1.0d, 2500L);

        assertEquals(2.0d, estimator.getSpeedMps(), 0.0001d);
        assertEquals(2.0d, state.getSpeedMetersPerSecond(), 0.0001d);
        assertTrue(estimator.getAvailability().isAvailable());
        assertEquals("车速估算已就绪", estimator.getAvailability().getMessage());
    }

    @Test
    public void updateSample_appliesZeroBiasAndZeroSpeedCorrection() {
        ImuSpeedEstimator estimator = new ImuSpeedEstimator();

        estimator.calibrate();
        // The first 5 samples are for calibration
        for (int i = 1; i <= 5; i++) {
            estimator.updateSample(0.1d, i * 100L);
        }

        // Give it some movement
        estimator.updateSample(1.1d, 600L);
        estimator.updateSample(1.1d, 700L);

        // Now provide enough still samples to fill the variance window (16) + required still samples (6)
        for (int i = 0; i < 25; i++) {
            estimator.updateSample(0.1d, 800L + (i * 100L));
        }

        assertEquals(0.0d, estimator.getSpeedMps(), 0.0001d);
        assertTrue(estimator.getAvailability().isAvailable());
    }

    @Test
    public void applyZeroSpeedCorrection_rejectsWhenGyroEnergyIsHigh() {
        ImuSpeedEstimator estimator = new ImuSpeedEstimator();

        calibrateAtRest(estimator);
        estimator.setGyroEnergy(0.5d);
        for (int i = 0; i < 25; i++) {
            estimator.updateSample(0.01d, 600L + (i * 100L));
        }

        assertTrue(estimator.getSpeedMps() > 0.0d);
    }

    @Test
    public void applyZeroSpeedCorrection_acceptsWhenGyroEnergyIsLow() {
        ImuSpeedEstimator estimator = new ImuSpeedEstimator();

        calibrateAtRest(estimator);
        estimator.updateSample(1.0d, 600L);

        estimator.setGyroEnergy(0.0d);
        for (int i = 0; i < 25; i++) {
            estimator.updateSample(0.0d, 700L + (i * 100L));
        }

        assertEquals(0.0d, estimator.getSpeedMps(), 0.0001d);
    }

    @Test
    public void onlineBiasEstimation_correctsResidualDriftOverTime() {
        ImuSpeedEstimator estimator = new ImuSpeedEstimator();

        calibrateAtRest(estimator);
        estimator.setGyroEnergy(0.0d);

        // Simulate a slow bias drift: residual 0.03 m/s² for many still samples
        for (int i = 0; i < 1000; i++) {
            estimator.updateSample(0.03d, 600L + i * 100L);
        }

        double speedAfterDrift = estimator.getSpeedMps();
        assertTrue("speed should be near zero after online bias correction, got " + speedAfterDrift,
                speedAfterDrift < 0.1d);
    }

    @Test
    public void updateSample_clampsNegativeSpeedAndRejectsInvalidSamples() {
        ImuSpeedEstimator estimator = new ImuSpeedEstimator();

        calibrateAtRest(estimator);
        ImuSpeedState state = estimator.updateSample(-2.0d, 600L);

        assertEquals(0.0d, state.getSpeedMetersPerSecond(), 0.0d);
        assertThrowsIllegalArgument(() -> estimator.updateSample(Double.NaN, 700L));
        assertThrowsIllegalArgument(() -> estimator.updateSample(0.0d, -1L));
    }

    @Test
    public void updateSample_reportsFallbackWhenCalibrationOrSamplingFails() {
        ImuSpeedEstimator estimator = new ImuSpeedEstimator();

        estimator.calibrate();
        estimator.updateSample(4.0d, 100L);
        estimator.updateSample(4.0d, 200L);
        estimator.updateSample(4.0d, 300L);
        estimator.updateSample(4.0d, 400L);
        estimator.updateSample(4.0d, 500L);
        assertFalse(estimator.getAvailability().isAvailable());
        assertEquals("车速校准失败：环境波动过大", estimator.getAvailability().getMessage());

        estimator.calibrate();
        calibrateAtRestWithoutStart(estimator);
        estimator.updateSample(1.0d, 1200L);
        estimator.updateSample(1.0d, 1800L);
        assertFalse(estimator.getAvailability().isAvailable());
        assertEquals("传感器采样异常", estimator.getAvailability().getMessage());
        assertEquals(0.0d, estimator.getSpeedMps(), 0.0d);
    }

    @Test
    public void reset_keepsEstimatorSafeAndRequiresCalibrationForNewInstance() {
        ImuSpeedEstimator estimator = new ImuSpeedEstimator();

        assertFalse(estimator.getAvailability().isAvailable());
        estimator.reset();
        assertFalse(estimator.getAvailability().isAvailable());
        calibrateAtRest(estimator);
        estimator.updateSample(1.0d, 600L);
        estimator.reset();

        assertEquals(0.0d, estimator.getSpeedMps(), 0.0d);
        assertTrue(estimator.getAvailability().isAvailable());
    }

    @Test
    public void resetAfterFailedCalibration_requiresNewSuccessfulCalibration() {
        ImuSpeedEstimator estimator = new ImuSpeedEstimator();

        calibrateWithBias(estimator, 4.0d);
        assertFalse(estimator.getAvailability().isAvailable());
        assertEquals("车速校准失败：环境波动过大", estimator.getAvailability().getMessage());

        estimator.reset();
        assertFalse(estimator.getAvailability().isAvailable());
        assertEquals("车速估算不可用", estimator.getAvailability().getMessage());
        estimator.updateSample(1.0d, 600L);
        assertEquals(0.0d, estimator.getSpeedMps(), 0.0d);
        assertFalse(estimator.getAvailability().isAvailable());

        calibrateAtRest(estimator);
        assertTrue(estimator.getAvailability().isAvailable());
        assertEquals("车速校准完成", estimator.getAvailability().getMessage());
    }

    private static void calibrateAtRest(ImuSpeedEstimator estimator) {
        estimator.calibrate();
        calibrateAtRestWithoutStart(estimator);
    }

    private static void calibrateAtRestWithoutStart(ImuSpeedEstimator estimator) {
        estimator.updateSample(0.0d, 100L);
        estimator.updateSample(0.0d, 200L);
        estimator.updateSample(0.0d, 300L);
        estimator.updateSample(0.0d, 400L);
        estimator.updateSample(0.0d, 500L);
    }

    private static void calibrateWithBias(ImuSpeedEstimator estimator, double biasMps2) {
        estimator.calibrate();
        estimator.updateSample(biasMps2, 100L);
        estimator.updateSample(biasMps2, 200L);
        estimator.updateSample(biasMps2, 300L);
        estimator.updateSample(biasMps2, 400L);
        estimator.updateSample(biasMps2, 500L);
    }

    private static void assertThrowsIllegalArgument(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private interface ThrowingRunnable {
        void run();
    }
}
