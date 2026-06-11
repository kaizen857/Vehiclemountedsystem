package com.example.vehicle_mountedsystem.ui.pages;

import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.vehicle_mountedsystem.R;
import com.example.vehicle_mountedsystem.data.sensor.MotionSensorProvider;
import com.example.vehicle_mountedsystem.data.speed.ImuSpeedEstimator;
import com.example.vehicle_mountedsystem.model.AvailabilityStatus;
import com.example.vehicle_mountedsystem.model.SensorReading;
import com.example.vehicle_mountedsystem.ui.AttitudeIndicatorView;
import com.example.vehicle_mountedsystem.util.AnimationHelper;

import java.util.Locale;

public final class SensorPageController {
    private static final long RENDER_THROTTLE_MILLIS = 80L;

    private final SensorReading accelerometerReading;
    private final SensorReading linearAccelerationReading;
    private final SensorReading gyroscopeReading;
    private final SensorReading rotationVectorReading;
    private final MotionSensorProvider sensorProvider;
    private final ImuSpeedEstimator imuSpeedEstimator;
    private View currentView;
    private View cachedPageView;
    private boolean renderScheduled;
    private long lastRenderUptimeMillis;

    public SensorPageController() {
        this(
                unavailable("加速度计", "m/s²"),
                unavailable("线性加速度", "m/s²"),
                unavailable("陀螺仪", "rad/s"),
                unavailable("旋转向量", "unitless"));
    }

    public SensorPageController(MotionSensorProvider sensorProvider) {
        this(sensorProvider, null);
    }

    public SensorPageController(MotionSensorProvider sensorProvider, ImuSpeedEstimator imuSpeedEstimator) {
        this.sensorProvider = sensorProvider;
        this.imuSpeedEstimator = imuSpeedEstimator;
        this.accelerometerReading = null;
        this.linearAccelerationReading = null;
        this.gyroscopeReading = null;
        this.rotationVectorReading = null;
    }

    SensorPageController(
            SensorReading accelerometerReading,
            SensorReading linearAccelerationReading,
            SensorReading gyroscopeReading,
            SensorReading rotationVectorReading) {
        this.sensorProvider = null;
        this.imuSpeedEstimator = null;
        this.accelerometerReading = accelerometerReading;
        this.linearAccelerationReading = linearAccelerationReading;
        this.gyroscopeReading = gyroscopeReading;
        this.rotationVectorReading = rotationVectorReading;
    }

    public View createView(ViewGroup parent) {
        if (cachedPageView == null) {
            cachedPageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_sensor, parent, false);
            wireImuControls(cachedPageView);
            AnimationHelper.playPageEnter(cachedPageView);
        } else if (cachedPageView.getParent() instanceof ViewGroup) {
            ((ViewGroup) cachedPageView.getParent()).removeView(cachedPageView);
        }

        currentView = cachedPageView;

        if (sensorProvider == null) {
            bind(cachedPageView, accelerometerReading, linearAccelerationReading, gyroscopeReading, rotationVectorReading);
        } else {
            sensorProvider.setReadingListener(provider -> {
                View target = currentView;
                updateAttitudeGyro(target);

                long now = SystemClock.uptimeMillis();
                if (target != null && !renderScheduled && now - lastRenderUptimeMillis >= RENDER_THROTTLE_MILLIS) {
                    renderScheduled = true;
                    lastRenderUptimeMillis = now;
                    target.post(() -> {
                        renderScheduled = false;
                        renderProviderReadings(target);
                    });
                }
            });
            renderProviderReadings(cachedPageView);
        }
        return cachedPageView;
    }

    private void wireImuControls(View view) {
        if (imuSpeedEstimator == null) {
            Button calibrateBtn = view.findViewById(R.id.sensorImuCalibrate);
            Button resetBtn = view.findViewById(R.id.sensorImuReset);
            if (calibrateBtn != null) calibrateBtn.setEnabled(false);
            if (resetBtn != null) resetBtn.setEnabled(false);
        } else {
            Button calibrateBtn = view.findViewById(R.id.sensorImuCalibrate);
            Button resetBtn = view.findViewById(R.id.sensorImuReset);
            if (calibrateBtn != null) {
                calibrateBtn.setOnClickListener(v -> imuSpeedEstimator.calibrate());
            }
            if (resetBtn != null) {
                resetBtn.setOnClickListener(v -> imuSpeedEstimator.reset());
            }
        }

        Button attitudeCalibrateBtn = view.findViewById(R.id.sensorAttitudeCalibrate);
        if (attitudeCalibrateBtn != null) {
            attitudeCalibrateBtn.setOnClickListener(v -> {
                AttitudeIndicatorView attitudeView = view.findViewById(R.id.sensorAttitudeIndicator);
                if (attitudeView != null) {
                    attitudeView.zeroCurrentAttitude();
                }
            });
        }

        Button flipPitchBtn = view.findViewById(R.id.sensorFlipPitch);
        if (flipPitchBtn != null) {
            flipPitchBtn.setOnClickListener(v -> {
                AttitudeIndicatorView attitudeView = view.findViewById(R.id.sensorAttitudeIndicator);
                if (attitudeView != null) {
                    attitudeView.flipPitch();
                    flipPitchBtn.setText(attitudeView.isPitchFlipped() ? "俯仰已翻转" : "翻转俯仰");
                }
            });
        }

        Button flipRollBtn = view.findViewById(R.id.sensorFlipRoll);
        if (flipRollBtn != null) {
            flipRollBtn.setOnClickListener(v -> {
                AttitudeIndicatorView attitudeView = view.findViewById(R.id.sensorAttitudeIndicator);
                if (attitudeView != null) {
                    attitudeView.flipRoll();
                    flipRollBtn.setText(attitudeView.isRollFlipped() ? "横滚已翻转" : "翻转横滚");
                }
            });
        }
    }

    public void stop() {
        if (sensorProvider != null) {
            sensorProvider.setReadingListener(null);
        }
        renderScheduled = false;
        lastRenderUptimeMillis = 0L;
        currentView = null;
    }

    private void renderProviderReadings(View view) {
        SensorReading rv = sensorProvider.getGameRotationVectorReading();
        if (rv == null || !rv.getAvailabilityStatus().isAvailable()) {
            rv = sensorProvider.getRotationVectorReading();
        }
        bind(view,
                sensorProvider.getAccelerometerReading(),
                sensorProvider.getLinearAccelerationReading(),
                sensorProvider.getGyroscopeReading(),
                rv);
        text(view, R.id.sensorTimestampValue, latestTimestampText());
    }

    private void updateAttitudeGyro(View view) {
        if (view == null) return;
        AttitudeIndicatorView attitudeView = view.findViewById(R.id.sensorAttitudeIndicator);
        if (attitudeView == null) return;

        SensorReading rotVec = sensorProvider.getGameRotationVectorReading();
        if (rotVec == null || !rotVec.getAvailabilityStatus().isAvailable()) {
            rotVec = sensorProvider.getRotationVectorReading();
            if (rotVec == null || !rotVec.getAvailabilityStatus().isAvailable()) {
                return;
            }
        }

        float x = (float) rotVec.getX();
        float y = (float) rotVec.getY();
        float z = (float) rotVec.getZ();
        float w = (float) Math.sqrt(Math.max(0.0, 1.0 - x * x - y * y - z * z));
        attitudeView.updateFromRotationVector(new float[]{x, y, z, w});
    }

    private void bind(
            View view,
            SensorReading accelerometerReading,
            SensorReading linearAccelerationReading,
            SensorReading gyroscopeReading,
            SensorReading rotationVectorReading) {
        bindReading(view, accelerometerReading, R.id.sensorAccelerometerValue, R.id.sensorAccelerometerStatus);
        bindReading(view, linearAccelerationReading, R.id.sensorLinearAccelerationValue, R.id.sensorLinearAccelerationStatus);
        bindReading(view, rotationVectorReading, R.id.sensorRotationVectorValue, R.id.sensorRotationVectorStatus);
    }

    private String latestTimestampText() {
        long latestTimestamp = Math.max(
                Math.max(timestamp(sensorProvider.getAccelerometerReading()),
                         timestamp(sensorProvider.getLinearAccelerationReading())),
                Math.max(timestamp(sensorProvider.getGameRotationVectorReading()),
                         timestamp(sensorProvider.getRotationVectorReading())));
        if (latestTimestamp == 0L) {
            return "等待传感器数据更新";
        }
        return String.format(Locale.US, "最近更新：%d ms", latestTimestamp);
    }

    private static long timestamp(SensorReading reading) {
        if (reading == null) return 0L;
        return reading.getAvailabilityStatus().getTimestampMillis();
    }

    private static void bindReading(View view, SensorReading reading, int valueId, int statusId) {
        text(view, valueId, axesText(reading));
        text(view, statusId, reading.getAvailabilityStatus().getMessage());
    }

    private static String axesText(SensorReading reading) {
        return String.format(
                Locale.US,
                "X %.2f · Y %.2f · Z %.2f %s",
                reading.getX(),
                reading.getY(),
                reading.getZ(),
                reading.getUnit());
    }

    private static SensorReading unavailable(String name, String unit) {
        return new SensorReading(name, 0.0d, 0.0d, 0.0d, unit, AvailabilityStatus.unavailable(name + "不可用", 0L));
    }

    private static void text(View view, int id, String value) {
        TextView textView = view.findViewById(id);
        if (textView != null) {
            textView.setText(value);
        }
    }
}
