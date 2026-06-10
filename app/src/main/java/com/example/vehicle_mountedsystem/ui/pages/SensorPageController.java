package com.example.vehicle_mountedsystem.ui.pages;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.vehicle_mountedsystem.R;
import com.example.vehicle_mountedsystem.model.AvailabilityStatus;
import com.example.vehicle_mountedsystem.model.SensorReading;

import java.util.Locale;

public final class SensorPageController {
    private final SensorReading accelerometerReading;
    private final SensorReading linearAccelerationReading;
    private final SensorReading gyroscopeReading;
    private final SensorReading rotationVectorReading;

    public SensorPageController() {
        this(
                unavailable("加速度计", "m/s²"),
                unavailable("线性加速度", "m/s²"),
                unavailable("陀螺仪", "rad/s"),
                unavailable("旋转向量", "unitless"));
    }

    SensorPageController(
            SensorReading accelerometerReading,
            SensorReading linearAccelerationReading,
            SensorReading gyroscopeReading,
            SensorReading rotationVectorReading) {
        this.accelerometerReading = accelerometerReading;
        this.linearAccelerationReading = linearAccelerationReading;
        this.gyroscopeReading = gyroscopeReading;
        this.rotationVectorReading = rotationVectorReading;
    }

    public View createView(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_sensor, parent, false);
        bind(view);
        return view;
    }

    private void bind(View view) {
        bindReading(view, accelerometerReading, R.id.sensorAccelerometerValue, R.id.sensorAccelerometerStatus);
        bindReading(view, linearAccelerationReading, R.id.sensorLinearAccelerationValue, R.id.sensorLinearAccelerationStatus);
        bindReading(view, gyroscopeReading, R.id.sensorGyroscopeValue, R.id.sensorGyroscopeStatus);
        bindReading(view, rotationVectorReading, R.id.sensorRotationVectorValue, R.id.sensorRotationVectorStatus);
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
        textView.setText(value);
    }
}
