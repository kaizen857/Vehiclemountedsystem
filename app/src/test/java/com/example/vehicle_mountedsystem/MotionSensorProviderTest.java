package com.example.vehicle_mountedsystem;

import com.example.vehicle_mountedsystem.data.sensor.MotionSensorProvider;
import com.example.vehicle_mountedsystem.model.SensorReading;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MotionSensorProviderTest {
    @Test
    public void start_exposesAllSupportedMotionReadingsAsPureModels() {
        FakeMotionSensorSource source = new FakeMotionSensorSource(
                MotionSensorProvider.SENSOR_ACCELEROMETER,
                MotionSensorProvider.SENSOR_LINEAR_ACCELERATION,
                MotionSensorProvider.SENSOR_GYROSCOPE,
                MotionSensorProvider.SENSOR_ROTATION_VECTOR);
        MotionSensorProvider provider = new MotionSensorProvider(source);

        provider.start();
        source.emit(new MotionSensorProvider.SensorSample(MotionSensorProvider.SENSOR_ACCELEROMETER, 1.0d, 2.0d, 3.0d, 100L));
        source.emit(new MotionSensorProvider.SensorSample(MotionSensorProvider.SENSOR_LINEAR_ACCELERATION, 0.5d, 0.0d, -0.5d, 110L));
        source.emit(new MotionSensorProvider.SensorSample(MotionSensorProvider.SENSOR_GYROSCOPE, 0.1d, 0.2d, 0.3d, 120L));
        source.emit(new MotionSensorProvider.SensorSample(MotionSensorProvider.SENSOR_ROTATION_VECTOR, 0.0d, 0.0d, 1.0d, 130L));

        SensorReading accelerometer = provider.getAccelerometerReading();
        SensorReading linearAcceleration = provider.getLinearAccelerationReading();
        SensorReading gyroscope = provider.getGyroscopeReading();
        SensorReading rotationVector = provider.getRotationVectorReading();

        assertTrue(source.started);
        assertArrayEquals(new int[]{
                MotionSensorProvider.SENSOR_ACCELEROMETER,
                MotionSensorProvider.SENSOR_LINEAR_ACCELERATION,
                MotionSensorProvider.SENSOR_GYROSCOPE,
                MotionSensorProvider.SENSOR_ROTATION_VECTOR
        }, source.requestedSensorTypes);
        assertEquals("加速度计", accelerometer.getName());
        assertEquals("m/s²", accelerometer.getUnit());
        assertEquals(1.0d, accelerometer.getX(), 0.0d);
        assertEquals(2.0d, accelerometer.getY(), 0.0d);
        assertEquals(3.0d, accelerometer.getZ(), 0.0d);
        assertEquals(100L, accelerometer.getAvailabilityStatus().getTimestampMillis());
        assertEquals("线性加速度", linearAcceleration.getName());
        assertEquals("m/s²", linearAcceleration.getUnit());
        assertEquals("陀螺仪", gyroscope.getName());
        assertEquals("rad/s", gyroscope.getUnit());
        assertEquals("旋转向量", rotationVector.getName());
        assertEquals("unitless", rotationVector.getUnit());
        assertTrue(rotationVector.getAvailabilityStatus().isAvailable());
    }

    @Test
    public void missingSensor_returnsUnavailableReadingWithoutCrashing() {
        FakeMotionSensorSource source = new FakeMotionSensorSource(MotionSensorProvider.SENSOR_ACCELEROMETER);
        MotionSensorProvider provider = new MotionSensorProvider(source);

        provider.start();

        assertTrue(provider.getAccelerometerReading().getAvailabilityStatus().isAvailable() == false);
        assertFalse(provider.getLinearAccelerationReading().getAvailabilityStatus().isAvailable());
        assertEquals("线性加速度不可用", provider.getLinearAccelerationReading().getAvailabilityStatus().getMessage());
        provider.stop();
        assertFalse(source.started);
    }

    private static final class FakeMotionSensorSource implements MotionSensorProvider.MotionSensorSource {
        private final Set<Integer> availableSensorTypes = new HashSet<>();
        private boolean started;
        private int[] requestedSensorTypes;
        private MotionSensorProvider.SourceListener listener;

        private FakeMotionSensorSource(int... availableSensorTypes) {
            for (int sensorType : availableSensorTypes) {
                this.availableSensorTypes.add(sensorType);
            }
        }

        @Override
        public boolean hasSensor(int sensorType) {
            return availableSensorTypes.contains(sensorType);
        }

        @Override
        public void start(MotionSensorProvider.SourceListener listener, int[] sensorTypes) {
            started = true;
            this.listener = listener;
            requestedSensorTypes = sensorTypes;
        }

        @Override
        public void stop() {
            started = false;
            listener = null;
        }

        private void emit(MotionSensorProvider.SensorSample sample) {
            listener.onSensorSample(sample);
        }
    }
}
