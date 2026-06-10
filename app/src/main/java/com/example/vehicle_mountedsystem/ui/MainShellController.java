package com.example.vehicle_mountedsystem.ui;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.vehicle_mountedsystem.R;
import com.example.vehicle_mountedsystem.data.hvac.HvacRepository;
import com.example.vehicle_mountedsystem.ui.pages.DashboardPageController;
import com.example.vehicle_mountedsystem.ui.pages.HvacPageController;
import com.example.vehicle_mountedsystem.ui.pages.MinimalVehiclePageController;
import com.example.vehicle_mountedsystem.ui.pages.NavigationPageController;
import com.example.vehicle_mountedsystem.ui.pages.SensorPageController;

public class MainShellController {

    private final View root;
    private final TextView pageTitleView;
    private final FrameLayout pageHostView;
    private final TabController tabController;
    private final DashboardPageController dashboardPageController;
    private final MinimalVehiclePageController minimalVehiclePageController;
    private final NavigationPageController navigationPageController;
    private final SensorPageController sensorPageController;
    private final HvacPageController hvacPageController;

    public MainShellController(View root) {
        this.root = root;
        this.pageTitleView = root.findViewById(R.id.pageTitle);
        this.pageHostView = root.findViewById(R.id.pageHost);
        this.dashboardPageController = new DashboardPageController();
        this.minimalVehiclePageController = new MinimalVehiclePageController();
        this.navigationPageController = new NavigationPageController();
        this.sensorPageController = new SensorPageController();
        this.hvacPageController = new HvacPageController(new HvacRepository(root.getContext()));
        this.tabController = new TabController(root, this::handleTabSelection);
        this.tabController.selectTab(TabController.Tab.OVERVIEW);
    }

    private void handleTabSelection(TabController.Tab tab) {
        if (pageTitleView != null) {
            pageTitleView.setText(tab.getTitleResId());
        }
        
        if (pageHostView == null) {
            return;
        }

        pageHostView.removeAllViews();
        View pageView = createPageView(tab);
        pageHostView.addView(pageView);
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
            default:
                return createPlaceholder(tab);
        }
    }

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
}
