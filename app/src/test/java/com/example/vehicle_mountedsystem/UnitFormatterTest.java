package com.example.vehicle_mountedsystem;

import com.example.vehicle_mountedsystem.model.AvailabilityStatus;
import com.example.vehicle_mountedsystem.util.UnitFormatter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UnitFormatterTest {
    @Test
    public void formatSpeedMetersPerSecond_usesMetersPerSecondUnit() {
        assertEquals("0.0 m/s", UnitFormatter.formatSpeedMetersPerSecond(0.0d));
        assertEquals("12.3 m/s", UnitFormatter.formatSpeedMetersPerSecond(12.34d));
        assertEquals("0.0 m/s", UnitFormatter.formatSpeedMetersPerSecond(-4.0d));
        assertEquals("--", UnitFormatter.formatSpeedMetersPerSecond(Double.NaN));
    }

    @Test
    public void formatBatteryPercent_clampsToPercentBounds() {
        assertEquals("0%", UnitFormatter.formatBatteryPercent(-3));
        assertEquals("56%", UnitFormatter.formatBatteryPercent(56));
        assertEquals("100%", UnitFormatter.formatBatteryPercent(130));
    }

    @Test
    public void formatMilliwatts_keepsSignedPowerValue() {
        assertEquals("1200 mW", UnitFormatter.formatMilliwatts(1200));
        assertEquals("-500 mW", UnitFormatter.formatMilliwatts(-500));
    }

    @Test
    public void formatGValue_usesTwoDecimalsAndUnavailableForInvalidValues() {
        assertEquals("0.00 G", UnitFormatter.formatGValue(0.0d));
        assertEquals("1.23 G", UnitFormatter.formatGValue(1.234d));
        assertEquals("--", UnitFormatter.formatGValue(Double.POSITIVE_INFINITY));
    }

    @Test
    public void formatUnavailable_usesStatusMessageOnlyWhenUnavailable() {
        assertEquals("无传感器", UnitFormatter.formatUnavailable(AvailabilityStatus.unavailable("无传感器", 1L)));
        assertEquals("--", UnitFormatter.formatUnavailable(AvailabilityStatus.available()));
        assertEquals("--", UnitFormatter.formatUnavailable(null));
    }

    @Test
    public void formatAvailabilityAware_returnsValueOnlyWhenAvailable() {
        assertEquals("88%", UnitFormatter.formatAvailabilityAware("88%", AvailabilityStatus.available("电池", 2L)));
        assertEquals("电池不可用", UnitFormatter.formatAvailabilityAware("88%", AvailabilityStatus.unavailable("电池不可用", 2L)));
        assertEquals("--", UnitFormatter.formatAvailabilityAware("88%", null));
    }
}
