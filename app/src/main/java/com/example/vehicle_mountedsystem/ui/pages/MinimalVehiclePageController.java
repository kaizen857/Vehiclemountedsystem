package com.example.vehicle_mountedsystem.ui.pages;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.vehicle_mountedsystem.R;
import com.example.vehicle_mountedsystem.model.BatteryStatus;
import com.example.vehicle_mountedsystem.model.ImuSpeedState;
import com.example.vehicle_mountedsystem.model.VehicleState;
import com.example.vehicle_mountedsystem.util.UnitFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MinimalVehiclePageController {
    private final VehicleState vehicleState;

    public MinimalVehiclePageController() {
        this(VehicleState.defaultState());
    }

    MinimalVehiclePageController(VehicleState vehicleState) {
        this.vehicleState = vehicleState;
    }

    public View createView(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_minimal_vehicle, parent, false);
        bind(view);
        return view;
    }

    private void bind(View view) {
        Context context = view.getContext();
        BatteryStatus batteryStatus = vehicleState.getBatteryStatus();
        ImuSpeedState imuSpeedState = vehicleState.getImuSpeedState();

        text(view, R.id.minimalSpeedValue, UnitFormatter.formatSpeedMetersPerSecond(imuSpeedState.getSpeedMetersPerSecond()));
        text(view, R.id.minimalGearValue, label(context, R.string.page_label_gear, vehicleState.getGearState().name()));
        text(view, R.id.minimalBatteryValue, label(context, R.string.page_label_battery, UnitFormatter.formatBatteryPercent(batteryStatus.getPercent())));
        text(view, R.id.minimalPowerValue, label(context, R.string.page_label_power, UnitFormatter.formatMilliwatts(batteryStatus.getPowerMilliwatts())));
        text(view, R.id.minimalHealthStatus, label(context, R.string.page_label_vehicle_health, context.getString(R.string.minimal_vehicle_health_normal)));
        text(view, R.id.minimalSensorAvailability, label(context, R.string.page_label_sensor_availability, context.getString(R.string.minimal_sensor_fallback)));
        text(view, R.id.minimalImuStatus, label(context, R.string.page_label_imu, imuSpeedState.getAvailabilityStatus().getMessage()));
        text(view, R.id.minimalSystemTime, label(context, R.string.page_label_system_time, currentTimeText()));
        text(view, R.id.minimalDemoMode, label(context, R.string.page_label_demo_mode, context.getString(R.string.minimal_demo_mode_value)));
    }

    private static String currentTimeText() {
        return new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date());
    }

    private static String label(Context context, int labelResId, String value) {
        return context.getString(labelResId) + "\n" + value;
    }

    private static void text(View view, int id, String value) {
        TextView textView = view.findViewById(id);
        textView.setText(value);
    }
}
