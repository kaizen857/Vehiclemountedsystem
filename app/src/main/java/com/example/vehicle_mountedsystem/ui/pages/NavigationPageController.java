package com.example.vehicle_mountedsystem.ui.pages;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.vehicle_mountedsystem.R;
import com.example.vehicle_mountedsystem.model.ImuSpeedState;
import com.example.vehicle_mountedsystem.model.VehicleState;
import com.example.vehicle_mountedsystem.util.UnitFormatter;

public final class NavigationPageController {
    private final VehicleState vehicleState;

    public NavigationPageController() {
        this(VehicleState.defaultState());
    }

    NavigationPageController(VehicleState vehicleState) {
        this.vehicleState = vehicleState;
    }

    public View createView(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_navigation, parent, false);
        bind(view);
        return view;
    }

    private void bind(View view) {
        Context context = view.getContext();
        ImuSpeedState imuSpeedState = vehicleState.getImuSpeedState();

        text(view, R.id.navigationSpeedValue, label(context, R.string.page_label_speed, UnitFormatter.formatSpeedMetersPerSecond(imuSpeedState.getSpeedMetersPerSecond())));
        text(view, R.id.navigationDestinationValue, label(context, R.string.page_label_destination, context.getString(R.string.navigation_demo_destination)));
        text(view, R.id.navigationDistanceValue, label(context, R.string.page_label_distance, context.getString(R.string.navigation_demo_distance)));
        text(view, R.id.navigationEtaValue, label(context, R.string.page_label_eta, context.getString(R.string.navigation_demo_eta)));
        text(view, R.id.navigationImuStatus, label(context, R.string.page_label_imu, imuSpeedState.getAvailabilityStatus().getMessage()));
    }

    private static String label(Context context, int labelResId, String value) {
        return context.getString(labelResId) + "\n" + value;
    }

    private static void text(View view, int id, String value) {
        TextView textView = view.findViewById(id);
        textView.setText(value);
    }
}
