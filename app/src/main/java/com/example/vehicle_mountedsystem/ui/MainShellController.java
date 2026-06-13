package com.example.vehicle_mountedsystem.ui;

import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.vehicle_mountedsystem.R;
import com.example.vehicle_mountedsystem.data.battery.BatteryStatusProvider;
import com.example.vehicle_mountedsystem.data.hvac.HvacRepository;
import com.example.vehicle_mountedsystem.data.media.SystemMediaController;
import com.example.vehicle_mountedsystem.data.sensor.MotionSensorProvider;
import com.example.vehicle_mountedsystem.data.speed.ImuSpeedEstimator;
import com.example.vehicle_mountedsystem.model.BatteryStatus;
import com.example.vehicle_mountedsystem.model.ImuSpeedState;
import com.example.vehicle_mountedsystem.model.SensorReading;
import com.example.vehicle_mountedsystem.ui.pages.DashboardPageController;
import com.example.vehicle_mountedsystem.ui.pages.HvacPageController;
import com.example.vehicle_mountedsystem.ui.pages.MediaPageController;
import com.example.vehicle_mountedsystem.ui.pages.MinimalVehiclePageController;
import com.example.vehicle_mountedsystem.ui.pages.NavigationPageController;
import com.example.vehicle_mountedsystem.ui.pages.SensorPageController;
import com.example.vehicle_mountedsystem.ui.pages.SettingsPageController;
import com.example.vehicle_mountedsystem.ui.pages.VehicleControlPageController;

public class MainShellController {

    private static final long DASHBOARD_REFRESH_INTERVAL_MILLIS = 200L;
    private static final long BATTERY_REFRESH_INTERVAL_MILLIS = 2000L;

    private final View root;
    private final TextView pageTitleView;
    private final FrameLayout pageHostView;
    private final TabController tabController;
    private final DashboardPageController dashboardPageController;
    private final MinimalVehiclePageController minimalVehiclePageController;
    private final NavigationPageController navigationPageController;
    private final SensorPageController sensorPageController;
    private final HvacPageController hvacPageController;
    private final MediaPageController mediaPageController;
    private final VehicleControlPageController vehicleControlPageController;
    private final SettingsPageController settingsPageController;

    // Shared data providers
    private final MotionSensorProvider motionSensorProvider;
    private final BatteryStatusProvider batteryStatusProvider;
    private final ImuSpeedEstimator imuSpeedEstimator;

    private final Handler uiHandler;
    private BatteryStatus cachedBatteryStatus;
    private ImuSpeedState cachedImuState;
    private double cachedLinearAccelX;
    private double cachedLinearAccelY;
    private boolean imuNeedsCalibration = true;

    private TabController.Tab currentTab;

    private final Runnable dashboardRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshDashboardData();
            uiHandler.postDelayed(this, DASHBOARD_REFRESH_INTERVAL_MILLIS);
        }
    };

    private final Runnable batteryRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshBatteryStatus();
            uiHandler.postDelayed(this, BATTERY_REFRESH_INTERVAL_MILLIS);
        }
    };

    public MainShellController(View root) {
        this.root = root;
        this.uiHandler = new Handler(Looper.getMainLooper());

        this.pageTitleView = root.findViewById(R.id.pageTitle);
        this.pageHostView = root.findViewById(R.id.pageHost);

        // Create shared data providers
        this.motionSensorProvider = new MotionSensorProvider(root.getContext());
        this.imuSpeedEstimator = new ImuSpeedEstimator();
        this.motionSensorProvider.setHighFrequencyListener(this.imuSpeedEstimator);
        this.motionSensorProvider.start();
        this.batteryStatusProvider = new BatteryStatusProvider(root.getContext());

        // Initialize cached states
        this.cachedImuState = new ImuSpeedState(0.0d, 0.0d,
                com.example.vehicle_mountedsystem.model.AvailabilityStatus.unavailable("正在启动系统", 0L));
        this.cachedBatteryStatus = batteryStatusProvider.readStatus(System.currentTimeMillis());

        // Create page controllers
        this.dashboardPageController = new DashboardPageController();
        this.minimalVehiclePageController = new MinimalVehiclePageController();
        this.navigationPageController = new NavigationPageController();
        this.sensorPageController = new SensorPageController(motionSensorProvider, imuSpeedEstimator);
        this.hvacPageController = new HvacPageController(new HvacRepository(root.getContext()));
        this.mediaPageController = new MediaPageController(new SystemMediaController(root.getContext()));
        this.vehicleControlPageController = new VehicleControlPageController();
        this.settingsPageController = new SettingsPageController();
        this.tabController = new TabController(root, this::handleTabSelection);
        this.tabController.selectTab(TabController.Tab.OVERVIEW);

        // Start periodic data refresh
        uiHandler.post(dashboardRefreshRunnable);
        uiHandler.post(batteryRefreshRunnable);
    }

    private void handleTabSelection(TabController.Tab tab) {
        stopCurrentPage();
        if (pageTitleView != null) {
            pageTitleView.setText(tab.getTitleResId());
        }

        if (pageHostView == null) {
            return;
        }

        pageHostView.removeAllViews();
        View pageView = createPageView(tab);
        pageHostView.addView(pageView);
        currentTab = tab;
    }

    private void stopCurrentPage() {
        if (currentTab == TabController.Tab.SENSORS) {
            sensorPageController.stop();
        } else if (currentTab == TabController.Tab.MEDIA) {
            mediaPageController.stop();
        }
    }

    private View createPageView(TabController.Tab tab) {
        switch (tab) {
            case OVERVIEW:
                return dashboardPageController.createView(pageHostView);
            case MINIMAL:
                return minimalVehiclePageController.createView(pageHostView);
            case NAVIGATION:
                return navigationPageController.createView(pageHostView);
            case SENSORS:
                return sensorPageController.createView(pageHostView);
            case HVAC:
                return hvacPageController.createView(pageHostView);
            case MEDIA:
                return mediaPageController.createView(pageHostView);
            case CONTROLS:
                return vehicleControlPageController.createView(pageHostView);
            case SETTINGS:
                return settingsPageController.createView(pageHostView);
            default:
                return createPlaceholder(tab);
        }
    }

    // --- Periodic data refresh ---

    private void refreshDashboardData() {
        if (imuNeedsCalibration) {
            imuSpeedEstimator.calibrate();
            imuNeedsCalibration = false;
        }

        cachedImuState = imuSpeedEstimator.getCurrentState();

        // If estimator is unavailable, use accelerometer for G-value fallback + direction
        if (!cachedImuState.getAvailabilityStatus().isAvailable()) {
            SensorReading accel = motionSensorProvider.getAccelerometerReading();
            if (accel != null && accel.getAvailabilityStatus().isAvailable()) {
                cachedLinearAccelX = accel.getX();
                cachedLinearAccelY = accel.getY();
                double gValue = Math.sqrt(
                        accel.getX() * accel.getX()
                                + accel.getY() * accel.getY()
                                + accel.getZ() * accel.getZ()) / 9.80665d;
                cachedImuState = new ImuSpeedState(0.0d, gValue,
                        com.example.vehicle_mountedsystem.model.AvailabilityStatus.available(
                                "合成 G 值已就绪", accel.getAvailabilityStatus().getTimestampMillis()));
            }
        } else {
            // Keep UI directions aligned if IMU is available, we might not have fwd/lat anymore.
            // We can just rely on raw accelerometer or linear accel for UI indicators.
            SensorReading linearAccel = motionSensorProvider.getLinearAccelerationReading();
            if (linearAccel != null && linearAccel.getAvailabilityStatus().isAvailable()) {
                cachedLinearAccelX = linearAccel.getX();
                cachedLinearAccelY = linearAccel.getY();
            }
        }
        dashboardPageController.refresh(cachedBatteryStatus, cachedImuState,
                cachedLinearAccelX, cachedLinearAccelY);
        minimalVehiclePageController.refresh(cachedBatteryStatus, cachedImuState);
        navigationPageController.refresh(cachedImuState);
    }

    private void refreshBatteryStatus() {
        // Battery status uses a sticky intent, so we can refresh in-place
        cachedBatteryStatus = batteryStatusProvider.readStatus(System.currentTimeMillis());
    }

    // --- Lifecycle ---

    private TextView createPlaceholder(TabController.Tab tab) {
        TextView placeholder = new TextView(root.getContext());
        placeholder.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        ));
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setText(tab.getPlaceholderResId());
        placeholder.setTextColor(root.getContext().getColor(R.color.text_secondary));
        placeholder.setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                root.getResources().getDimension(R.dimen.shell_placeholder_text_size)
        );
        return placeholder;
    }

    public TabController getTabController() {
        return tabController;
    }

    public void destroy() {
        uiHandler.removeCallbacks(dashboardRefreshRunnable);
        uiHandler.removeCallbacks(batteryRefreshRunnable);
        stopCurrentPage();
        motionSensorProvider.stop();
        currentTab = null;
    }
}
