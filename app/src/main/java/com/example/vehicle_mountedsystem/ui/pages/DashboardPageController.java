package com.example.vehicle_mountedsystem.ui.pages;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.vehicle_mountedsystem.R;
import com.example.vehicle_mountedsystem.model.BatteryStatus;
import com.example.vehicle_mountedsystem.model.HvacState;
import com.example.vehicle_mountedsystem.model.ImuSpeedState;
import com.example.vehicle_mountedsystem.model.VehicleState;
import com.example.vehicle_mountedsystem.util.UnitFormatter;

public final class DashboardPageController {
    private final VehicleState vehicleState;

    public DashboardPageController() {
        this(VehicleState.defaultState());
    }

    DashboardPageController(VehicleState vehicleState) {
        this.vehicleState = vehicleState;
    }

    public View createView(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_dashboard, parent, false);
        bind(view);
        return view;
    }

    private void bind(View view) {
        Context context = view.getContext();
        BatteryStatus batteryStatus = vehicleState.getBatteryStatus();
        ImuSpeedState imuSpeedState = vehicleState.getImuSpeedState();
        HvacState hvacState = vehicleState.getHvacState();

        text(view, R.id.dashboardSpeedValue, UnitFormatter.formatSpeedMetersPerSecond(imuSpeedState.getSpeedMetersPerSecond()));
        text(view, R.id.dashboardImuValue, label(context, R.string.page_label_imu, imuSpeedState.getAvailabilityStatus().getMessage()));
        text(view, R.id.dashboardGearValue, vehicleState.getGearState().name());
        text(view, R.id.dashboardBatteryValue, UnitFormatter.formatBatteryPercent(batteryStatus.getPercent()));
        text(view, R.id.dashboardPowerValue, label(context, R.string.page_label_power, UnitFormatter.formatMilliwatts(batteryStatus.getPowerMilliwatts())));
        text(view, R.id.dashboardGValue, label(context, R.string.page_label_g_value, UnitFormatter.formatGValue(imuSpeedState.getAccelerationG())));
        text(view, R.id.dashboardNavigationSummary, label(context, R.string.page_label_navigation, context.getString(R.string.navigation_demo_summary)));
        text(view, R.id.dashboardHvacSummary, label(context, R.string.page_label_hvac, hvacSummary(hvacState)));
    }

    private static String hvacSummary(HvacState hvacState) {
        String acText = hvacState.isAcEnabled() ? "AC 开启" : "AC 关闭";
        String circulationText = hvacState.isInnerCirculationEnabled() ? "内循环" : "外循环";
        return hvacState.getTemperatureCelsius() + "°C · 风量 "
                + hvacState.getFanLevel() + " · "
                + hvacState.getModeLabel() + " · "
                + acText + " · " + circulationText;
    }

    private static String label(Context context, int labelResId, String value) {
        return context.getString(labelResId) + "\n" + value;
    }

    private static void text(View view, int id, String value) {
        TextView textView = view.findViewById(id);
        textView.setText(value);
    }
}
