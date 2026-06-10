package com.example.vehicle_mountedsystem;

import com.example.vehicle_mountedsystem.model.AvailabilityStatus;
import com.example.vehicle_mountedsystem.model.BatteryStatus;
import com.example.vehicle_mountedsystem.model.GearState;
import com.example.vehicle_mountedsystem.model.HvacState;
import com.example.vehicle_mountedsystem.model.ImuSpeedState;
import com.example.vehicle_mountedsystem.model.MediaState;
import com.example.vehicle_mountedsystem.model.SensorReading;
import com.example.vehicle_mountedsystem.model.VehicleState;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class VehicleModelTest {
    @Test
    public void vehicleDefaultState_usesStableDefaults() {
        VehicleState state = VehicleState.defaultState();

        assertEquals(0.0d, state.getSpeedMetersPerSecond(), 0.0d);
        assertSame(GearState.P, state.getGearState());
        assertEquals(24, state.getHvacState().getTemperatureCelsius());
        assertEquals(2, state.getHvacState().getFanLevel());
        assertEquals("AUTO", state.getHvacState().getMode());
        assertEquals("自动", state.getHvacState().getModeLabel());
        assertFalse(state.getHvacState().isAcEnabled());
        assertFalse(state.getHvacState().isInnerCirculationEnabled());
        assertFalse(state.getMediaState().getAvailabilityStatus().isAvailable());
    }

    @Test
    public void availabilityStatusDefaults_areDeterministic() {
        AvailabilityStatus available = AvailabilityStatus.available();
        AvailabilityStatus unavailable = AvailabilityStatus.unavailable();

        assertTrue(available.isAvailable());
        assertEquals("可用", available.getMessage());
        assertEquals(0L, available.getTimestampMillis());
        assertFalse(unavailable.isAvailable());
        assertEquals("不可用", unavailable.getMessage());
        assertEquals(0L, unavailable.getTimestampMillis());
    }

    @Test
    public void availabilityStatusRejectsBlankMessageAndNegativeTimestamp() {
        assertThrowsIllegalArgument(() -> AvailabilityStatus.available(" ", 0L));
        assertThrowsIllegalArgument(() -> AvailabilityStatus.unavailable("无数据", -1L));
    }

    @Test
    public void gearStateAcceptsOnlyKnownValues() {
        assertSame(GearState.P, GearState.defaultState());
        assertSame(GearState.R, GearState.fromValue("R"));
        assertSame(GearState.S, GearState.fromValue(" S "));

        assertThrowsIllegalArgument(() -> GearState.fromValue("L"));
        assertThrowsIllegalArgument(() -> GearState.fromValue(null));
    }

    @Test
    public void hvacStateClampsTemperatureAndFanBounds() {
        HvacState low = new HvacState(10, -1, "AUTO", "自动", false, false);
        HvacState high = new HvacState(35, 9, "AUTO", "自动", true, true);

        assertEquals(16, low.getTemperatureCelsius());
        assertEquals(0, low.getFanLevel());
        assertEquals(30, high.getTemperatureCelsius());
        assertEquals(5, high.getFanLevel());
        assertTrue(high.isAcEnabled());
        assertTrue(high.isInnerCirculationEnabled());
    }

    @Test
    public void hvacStateRejectsBlankModeText() {
        assertThrowsIllegalArgument(() -> new HvacState(24, 2, "", "自动", false, false));
        assertThrowsIllegalArgument(() -> new HvacState(24, 2, "AUTO", null, false, false));
    }

    @Test
    public void batteryStatusClampsPercentAndRequiresAvailability() {
        BatteryStatus low = new BatteryStatus(-20, 1200, AvailabilityStatus.available());
        BatteryStatus high = new BatteryStatus(150, -500, AvailabilityStatus.available("放电", 5L));

        assertEquals(0, low.getPercent());
        assertEquals(1200, low.getPowerMilliwatts());
        assertEquals(100, high.getPercent());
        assertEquals(-500, high.getPowerMilliwatts());
        assertThrowsNullPointer(() -> new BatteryStatus(50, 0, null));
    }

    @Test
    public void imuSpeedStateRejectsInvalidNumbers() {
        ImuSpeedState state = new ImuSpeedState(12.5d, -0.25d, AvailabilityStatus.available());

        assertEquals(12.5d, state.getSpeedMetersPerSecond(), 0.0d);
        assertEquals(-0.25d, state.getAccelerationG(), 0.0d);
        assertThrowsIllegalArgument(() -> new ImuSpeedState(-1.0d, 0.0d, AvailabilityStatus.available()));
        assertThrowsIllegalArgument(() -> new ImuSpeedState(Double.NaN, 0.0d, AvailabilityStatus.available()));
        assertThrowsIllegalArgument(() -> new ImuSpeedState(0.0d, Double.POSITIVE_INFINITY, AvailabilityStatus.available()));
        assertThrowsNullPointer(() -> new ImuSpeedState(0.0d, 0.0d, null));
    }

    @Test
    public void mediaStateDefaultIsUnavailableAndRejectsBlankText() {
        MediaState state = MediaState.defaultState();

        assertEquals("无媒体", state.getTitle());
        assertEquals("未知艺术家", state.getArtist());
        assertFalse(state.isPlaying());
        assertFalse(state.getAvailabilityStatus().isAvailable());
        assertThrowsIllegalArgument(() -> new MediaState("", "artist", false, AvailabilityStatus.available()));
        assertThrowsIllegalArgument(() -> new MediaState("title", " ", false, AvailabilityStatus.available()));
        assertThrowsNullPointer(() -> new MediaState("title", "artist", false, null));
    }

    @Test
    public void sensorReadingStoresAxisValuesAndUnavailableDefaults() {
        SensorReading reading = new SensorReading("加速度", 0.1d, -0.2d, 9.8d, "m/s²", AvailabilityStatus.available("实时", 10L));
        SensorReading unavailable = SensorReading.unavailable("陀螺仪", "rad/s");

        assertEquals("加速度", reading.getName());
        assertEquals(0.1d, reading.getX(), 0.0d);
        assertEquals(-0.2d, reading.getY(), 0.0d);
        assertEquals(9.8d, reading.getZ(), 0.0d);
        assertEquals("m/s²", reading.getUnit());
        assertTrue(reading.getAvailabilityStatus().isAvailable());
        assertEquals(0.0d, unavailable.getX(), 0.0d);
        assertEquals(0.0d, unavailable.getY(), 0.0d);
        assertEquals(0.0d, unavailable.getZ(), 0.0d);
        assertFalse(unavailable.getAvailabilityStatus().isAvailable());
    }

    @Test
    public void sensorReadingRejectsInvalidAxisValuesAndRequiredFields() {
        assertThrowsIllegalArgument(() -> new SensorReading("", 1.0d, 2.0d, 3.0d, "G", AvailabilityStatus.available()));
        assertThrowsIllegalArgument(() -> new SensorReading("加速度", Double.NaN, 2.0d, 3.0d, "G", AvailabilityStatus.available()));
        assertThrowsIllegalArgument(() -> new SensorReading("加速度", 1.0d, Double.POSITIVE_INFINITY, 3.0d, "G", AvailabilityStatus.available()));
        assertThrowsIllegalArgument(() -> new SensorReading("加速度", 1.0d, 2.0d, Double.NEGATIVE_INFINITY, "G", AvailabilityStatus.available()));
        assertThrowsIllegalArgument(() -> new SensorReading("加速度", 1.0d, 2.0d, 3.0d, "", AvailabilityStatus.available()));
        assertThrowsNullPointer(() -> new SensorReading("加速度", 1.0d, 2.0d, 3.0d, "G", null));
    }

    @Test
    public void vehicleStateRejectsInvalidSpeedAndNullChildren() {
        VehicleState state = new VehicleState(
                8.0d,
                GearState.D,
                HvacState.defaultState(),
                BatteryStatus.defaultState(),
                ImuSpeedState.defaultState(),
                MediaState.defaultState());

        assertEquals(8.0d, state.getSpeedMetersPerSecond(), 0.0d);
        assertSame(GearState.D, state.getGearState());
        assertThrowsIllegalArgument(() -> new VehicleState(
                -0.1d,
                GearState.P,
                HvacState.defaultState(),
                BatteryStatus.defaultState(),
                ImuSpeedState.defaultState(),
                MediaState.defaultState()));
        assertThrowsNullPointer(() -> new VehicleState(
                0.0d,
                null,
                HvacState.defaultState(),
                BatteryStatus.defaultState(),
                ImuSpeedState.defaultState(),
                MediaState.defaultState()));
    }

    private static void assertThrowsIllegalArgument(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private static void assertThrowsNullPointer(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (NullPointerException expected) {
            return;
        }
        throw new AssertionError("Expected NullPointerException");
    }

    private interface ThrowingRunnable {
        void run();
    }
}
