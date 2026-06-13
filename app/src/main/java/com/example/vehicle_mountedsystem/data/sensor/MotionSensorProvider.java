package com.example.vehicle_mountedsystem.data.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;

import com.example.vehicle_mountedsystem.model.AvailabilityStatus;
import com.example.vehicle_mountedsystem.model.SensorReading;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class MotionSensorProvider {
    public static final int SENSOR_ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    public static final int SENSOR_LINEAR_ACCELERATION = Sensor.TYPE_LINEAR_ACCELERATION;
    public static final int SENSOR_GYROSCOPE = Sensor.TYPE_GYROSCOPE;
    public static final int SENSOR_ROTATION_VECTOR = Sensor.TYPE_ROTATION_VECTOR;
    public static final int SENSOR_GAME_ROTATION_VECTOR = Sensor.TYPE_GAME_ROTATION_VECTOR;

    private static final int[] REQUIRED_SENSORS = {
            SENSOR_ACCELEROMETER,
            SENSOR_LINEAR_ACCELERATION,
            SENSOR_GYROSCOPE,
            SENSOR_ROTATION_VECTOR,
            SENSOR_GAME_ROTATION_VECTOR
    };
    private static final String ACCELEROMETER_NAME = "加速度计";
    private static final String LINEAR_ACCELERATION_NAME = "线性加速度";
    private static final String GYROSCOPE_NAME = "陀螺仪";
    private static final String ROTATION_VECTOR_NAME = "旋转向量";
    private static final String GAME_ROTATION_VECTOR_NAME = "游戏旋转向量";
    private static final String ACCELERATION_UNIT = "m/s²";
    private static final String GYROSCOPE_UNIT = "rad/s";
    private static final String ROTATION_UNIT = "unitless";

    private final MotionSensorSource sensorSource;
    private final Map<Integer, SensorReading> readings = new HashMap<>();
    private ReadingListener readingListener;
    private SourceListener highFrequencyListener;

    public MotionSensorProvider(Context context) {
        this(new AndroidMotionSensorSource(context));
    }

    public MotionSensorProvider(MotionSensorSource sensorSource) {
        this.sensorSource = Objects.requireNonNull(sensorSource, "sensorSource");
        resetUnavailableReadings();
    }

    public synchronized void start() {
        resetUnavailableReadings();
        sensorSource.start(new SourceListener() {
            @Override
            public void onSensorSample(SensorSample sample) {
                onSample(sample);
            }
        }, Arrays.copyOf(REQUIRED_SENSORS, REQUIRED_SENSORS.length));
        for (int sensorType : REQUIRED_SENSORS) {
            if (sensorSource.hasSensor(sensorType)) {
                readings.put(sensorType, waitingReading(sensorType));
            } else {
                readings.put(sensorType, unavailableReading(sensorType));
            }
        }
        notifyReadingsChanged();
    }

    public void stop() {
        sensorSource.stop();
    }

    public synchronized void setReadingListener(ReadingListener readingListener) {
        this.readingListener = readingListener;
    }

    public synchronized void setHighFrequencyListener(SourceListener listener) {
        this.highFrequencyListener = listener;
    }

    public synchronized void onSample(SensorSample sample) {
        Objects.requireNonNull(sample, "sample");
        SourceListener hfListener = highFrequencyListener;
        if (hfListener != null) {
            hfListener.onSensorSample(sample);
        }
        if (!isSupportedSensor(sample.getSensorType())) {
            return;
        }
        readings.put(sample.getSensorType(), toReading(sample));
        notifyReadingsChanged();
    }

    private void notifyReadingsChanged() {
        if (readingListener != null) {
            readingListener.onReadingsChanged(this);
        }
    }

    public synchronized SensorReading getAccelerometerReading() {
        return readings.get(SENSOR_ACCELEROMETER);
    }

    public synchronized SensorReading getLinearAccelerationReading() {
        return readings.get(SENSOR_LINEAR_ACCELERATION);
    }

    public synchronized SensorReading getGyroscopeReading() {
        return readings.get(SENSOR_GYROSCOPE);
    }

    public synchronized SensorReading getRotationVectorReading() {
        return readings.get(SENSOR_ROTATION_VECTOR);
    }

    public synchronized SensorReading getGameRotationVectorReading() {
        return readings.get(SENSOR_GAME_ROTATION_VECTOR);
    }

    private SensorReading toReading(SensorSample sample) {
        return new SensorReading(
                nameFor(sample.getSensorType()),
                sample.getX(),
                sample.getY(),
                sample.getZ(),
                unitFor(sample.getSensorType()),
                AvailabilityStatus.available("数据流正常", sample.getTimestampMillis()));
    }

    private void resetUnavailableReadings() {
        for (int sensorType : REQUIRED_SENSORS) {
            readings.put(sensorType, unavailableReading(sensorType));
        }
    }

    private static SensorReading unavailableReading(int sensorType) {
        return new SensorReading(
                nameFor(sensorType),
                0.0d,
                0.0d,
                0.0d,
                unitFor(sensorType),
                AvailabilityStatus.unavailable(nameFor(sensorType) + "未就绪", 0L));
    }

    private static SensorReading waitingReading(int sensorType) {
        return new SensorReading(
                nameFor(sensorType),
                0.0d,
                0.0d,
                0.0d,
                unitFor(sensorType),
                AvailabilityStatus.available("等待" + nameFor(sensorType) + "同步", 0L));
    }

    private static boolean isSupportedSensor(int sensorType) {
        for (int requiredSensor : REQUIRED_SENSORS) {
            if (requiredSensor == sensorType) {
                return true;
            }
        }
        return false;
    }

    private static String nameFor(int sensorType) {
        if (sensorType == SENSOR_ACCELEROMETER) {
            return ACCELEROMETER_NAME;
        }
        if (sensorType == SENSOR_LINEAR_ACCELERATION) {
            return LINEAR_ACCELERATION_NAME;
        }
        if (sensorType == SENSOR_GYROSCOPE) {
            return GYROSCOPE_NAME;
        }
        if (sensorType == SENSOR_ROTATION_VECTOR) {
            return ROTATION_VECTOR_NAME;
        }
        if (sensorType == SENSOR_GAME_ROTATION_VECTOR) {
            return GAME_ROTATION_VECTOR_NAME;
        }
        throw new IllegalArgumentException("Unsupported sensor type: " + sensorType);
    }

    private static String unitFor(int sensorType) {
        if (sensorType == SENSOR_ACCELEROMETER || sensorType == SENSOR_LINEAR_ACCELERATION) {
            return ACCELERATION_UNIT;
        }
        if (sensorType == SENSOR_GYROSCOPE) {
            return GYROSCOPE_UNIT;
        }
        if (sensorType == SENSOR_ROTATION_VECTOR || sensorType == SENSOR_GAME_ROTATION_VECTOR) {
            return ROTATION_UNIT;
        }
        throw new IllegalArgumentException("Unsupported sensor type: " + sensorType);
    }

    public interface MotionSensorSource {
        boolean hasSensor(int sensorType);

        void start(SourceListener listener, int[] sensorTypes);

        void stop();
    }

    public interface SourceListener {
        void onSensorSample(SensorSample sample);
    }

    public interface ReadingListener {
        void onReadingsChanged(MotionSensorProvider provider);
    }

    public static final class SensorSample {
        private final int sensorType;
        private final double x;
        private final double y;
        private final double z;
        private final long timestampMillis;

        public SensorSample(int sensorType, double x, double y, double z, long timestampMillis) {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("sensor axis values must be finite");
            }
            if (timestampMillis < 0L) {
                throw new IllegalArgumentException("timestampMillis must be >= 0");
            }
            this.sensorType = sensorType;
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestampMillis = timestampMillis;
        }

        public int getSensorType() {
            return sensorType;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public long getTimestampMillis() {
            return timestampMillis;
        }
    }

    private static final class AndroidMotionSensorSource implements MotionSensorSource, SensorEventListener {
        private final SensorManager sensorManager;
        private SourceListener listener;
        private HandlerThread sensorThread;

        private AndroidMotionSensorSource(Context context) {
            Context appContext = Objects.requireNonNull(context, "context").getApplicationContext();
            sensorManager = (SensorManager) appContext.getSystemService(Context.SENSOR_SERVICE);
        }

        @Override
        public boolean hasSensor(int sensorType) {
            return sensorManager != null && sensorManager.getDefaultSensor(sensorType) != null;
        }

        @Override
        public void start(SourceListener listener, int[] sensorTypes) {
            this.listener = Objects.requireNonNull(listener, "listener");
            if (sensorManager == null) {
                return;
            }
            sensorThread = new HandlerThread("vehicle-motion-sensors");
            sensorThread.start();
            Handler handler = new Handler(sensorThread.getLooper());
            for (int sensorType : sensorTypes) {
                Sensor sensor = sensorManager.getDefaultSensor(sensorType);
                if (sensor != null) {
                    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST, handler);
                }
            }
        }

        @Override
        public void stop() {
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
            if (sensorThread != null) {
                sensorThread.quitSafely();
                sensorThread = null;
            }
            listener = null;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (listener == null || event == null || event.sensor == null || event.values == null) {
                return;
            }
            double x = event.values.length > 0 ? event.values[0] : 0.0d;
            double y = event.values.length > 1 ? event.values[1] : 0.0d;
            double z = event.values.length > 2 ? event.values[2] : 0.0d;
            listener.onSensorSample(new SensorSample(event.sensor.getType(), x, y, z, event.timestamp / 1_000_000L));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}
