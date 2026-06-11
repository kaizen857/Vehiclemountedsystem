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
import com.example.vehicle_mountedsystem.ui.GForceIndicatorView;
import com.example.vehicle_mountedsystem.util.AnimationHelper;
import com.example.vehicle_mountedsystem.util.UnitFormatter;

public final class DashboardPageController {
    private VehicleState vehicleState;
    private View dashboardView;

    public DashboardPageController() {
        this(VehicleState.defaultState());
    }

    DashboardPageController(VehicleState vehicleState) {
        this.vehicleState = vehicleState;
    }

    public View createView(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_dashboard, parent, false);
        dashboardView = view;
        bind(view);
        AnimationHelper.playPageEnter(view);
        return view;
    }

    /**
     * Called by MainShellController to push live data into the dashboard.
     */
    public void refresh(BatteryStatus batteryStatus, ImuSpeedState imuSpeedState,
                        double linearAccelX, double linearAccelY) {
        VehicleState updated = new VehicleState(
                imuSpeedState.getSpeedMetersPerSecond(),
                vehicleState.getGearState(),
                vehicleState.getHvacState(),
                batteryStatus,
                imuSpeedState,
                vehicleState.getMediaState());
        this.vehicleState = updated;
        if (dashboardView != null) {
            bind(dashboardView);
            GForceIndicatorView gForceView = dashboardView.findViewById(R.id.dashboardGForceIndicator);
            if (gForceView != null) {
                gForceView.updateGForce(
                        (float) (linearAccelY / 9.80665d),
                        (float) (linearAccelX / 9.80665d));
            }
        }
    }

    /**
     * Update HVAC state from the HvacPageController (shared via MainShellController).
     */
    public void updateHvac(HvacState hvacState) {
        VehicleState updated = new VehicleState(
                vehicleState.getSpeedMetersPerSecond(),
                vehicleState.getGearState(),
                hvacState,
                vehicleState.getBatteryStatus(),
                vehicleState.getImuSpeedState(),
                vehicleState.getMediaState());
        this.vehicleState = updated;
        if (dashboardView != null) {
            bind(dashboardView);
        }
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
        if (textView != null) {
            textView.setText(value);
        }
    }
}
