package com.example.vehicle_mountedsystem.data.battery;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.example.vehicle_mountedsystem.model.AvailabilityStatus;
import com.example.vehicle_mountedsystem.model.BatteryStatus;

import java.util.Objects;

public final class BatteryStatusProvider {
    private static final int UNSUPPORTED_INT = Integer.MIN_VALUE;
    private static final String BATTERY_AVAILABLE_MESSAGE = "动力系统电量正常";
    private static final String POWER_UNAVAILABLE_MESSAGE = "电池电量已就绪，功率估算同步中";
    private static final String BATTERY_UNAVAILABLE_MESSAGE = "动力系统状态不可用";

    private final BatteryDataSource dataSource;

    public BatteryStatusProvider(Context context) {
        this(new AndroidBatteryDataSource(context));
    }

    public BatteryStatusProvider(BatteryDataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    public BatteryStatus readStatus(long timestampMillis) {
        if (timestampMillis < 0L) {
            throw new IllegalArgumentException("timestampMillis must be >= 0");
        }
        BatterySnapshot snapshot = dataSource.readSnapshot();
        if (snapshot == null || !isValidLevel(snapshot.getLevel(), snapshot.getScale())) {
            return new BatteryStatus(0, 0, AvailabilityStatus.unavailable(BATTERY_UNAVAILABLE_MESSAGE, timestampMillis));
        }

        int percent = (int) Math.round(snapshot.getLevel() * 100.0d / snapshot.getScale());
        Integer powerMilliwatts = estimatePowerMilliwatts(snapshot.getCurrentMicroamps(), snapshot.getVoltageMillivolts());
        if (powerMilliwatts == null) {
            return new BatteryStatus(percent, 0, AvailabilityStatus.available(POWER_UNAVAILABLE_MESSAGE, timestampMillis));
        }
        return new BatteryStatus(percent, powerMilliwatts, AvailabilityStatus.available(BATTERY_AVAILABLE_MESSAGE, timestampMillis));
    }

    private static boolean isValidLevel(int level, int scale) {
        return level >= 0 && scale > 0;
    }

    private static Integer estimatePowerMilliwatts(int currentMicroamps, int voltageMillivolts) {
        if (currentMicroamps == UNSUPPORTED_INT || voltageMillivolts <= 0) {
            return null;
        }
        long powerMilliwatts = (long) currentMicroamps * (long) voltageMillivolts / 1_000_000L;
        if (powerMilliwatts > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (powerMilliwatts < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) powerMilliwatts;
    }

    public interface BatteryDataSource {
        BatterySnapshot readSnapshot();
    }

    public static final class BatterySnapshot {
        private final int level;
        private final int scale;
        private final int currentMicroamps;
        private final int voltageMillivolts;

        public BatterySnapshot(int level, int scale, int currentMicroamps, int voltageMillivolts) {
            this.level = level;
            this.scale = scale;
            this.currentMicroamps = currentMicroamps;
            this.voltageMillivolts = voltageMillivolts;
        }

        public int getLevel() {
            return level;
        }

        public int getScale() {
            return scale;
        }

        public int getCurrentMicroamps() {
            return currentMicroamps;
        }

        public int getVoltageMillivolts() {
            return voltageMillivolts;
        }
    }

    private static final class AndroidBatteryDataSource implements BatteryDataSource {
        private final Context context;
        private final BatteryManager batteryManager;

        private AndroidBatteryDataSource(Context context) {
            this.context = Objects.requireNonNull(context, "context").getApplicationContext();
            batteryManager = this.context.getSystemService(BatteryManager.class);
        }

        @Override
        public BatterySnapshot readSnapshot() {
            Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (intent == null) {
                return null;
            }
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int voltageMillivolts = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, UNSUPPORTED_INT);
            int currentMicroamps = batteryManager == null
                    ? UNSUPPORTED_INT
                    : batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            return new BatterySnapshot(level, scale, currentMicroamps, voltageMillivolts);
        }
    }
}
