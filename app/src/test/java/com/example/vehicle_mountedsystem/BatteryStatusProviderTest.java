package com.example.vehicle_mountedsystem;

import com.example.vehicle_mountedsystem.data.battery.BatteryStatusProvider;
import com.example.vehicle_mountedsystem.model.BatteryStatus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BatteryStatusProviderTest {
    @Test
    public void readStatus_mapsLevelScaleCurrentAndVoltageToBatteryStatus() {
        BatteryStatusProvider provider = new BatteryStatusProvider(
                () -> new BatteryStatusProvider.BatterySnapshot(45, 90, -500_000, 4_000));

        BatteryStatus status = provider.readStatus(22L);

        assertEquals(50, status.getPercent());
        assertEquals(-2000, status.getPowerMilliwatts());
        assertTrue(status.getAvailabilityStatus().isAvailable());
        assertEquals("动力系统电量正常", status.getAvailabilityStatus().getMessage());
        assertEquals(22L, status.getAvailabilityStatus().getTimestampMillis());
    }

    @Test
    public void readStatus_keepsBatteryAvailableWhenPowerPathUnsupported() {
        BatteryStatusProvider provider = new BatteryStatusProvider(
                () -> new BatteryStatusProvider.BatterySnapshot(80, 100, Integer.MIN_VALUE, 0));

        BatteryStatus status = provider.readStatus(30L);

        assertEquals(80, status.getPercent());
        assertEquals(0, status.getPowerMilliwatts());
        assertTrue(status.getAvailabilityStatus().isAvailable());
        assertEquals("电池电量已就绪，功率估算同步中", status.getAvailabilityStatus().getMessage());
    }

    @Test
    public void readStatus_returnsUnavailableWhenBatteryIntentMissingOrInvalid() {
        BatteryStatusProvider missingProvider = new BatteryStatusProvider(() -> null);
        BatteryStatusProvider invalidProvider = new BatteryStatusProvider(
                () -> new BatteryStatusProvider.BatterySnapshot(-1, 0, Integer.MIN_VALUE, Integer.MIN_VALUE));

        BatteryStatus missing = missingProvider.readStatus(40L);
        BatteryStatus invalid = invalidProvider.readStatus(41L);

        assertFalse(missing.getAvailabilityStatus().isAvailable());
        assertEquals("动力系统状态不可用", missing.getAvailabilityStatus().getMessage());
        assertEquals(0, missing.getPercent());
        assertFalse(invalid.getAvailabilityStatus().isAvailable());
        assertEquals(41L, invalid.getAvailabilityStatus().getTimestampMillis());
    }
}
