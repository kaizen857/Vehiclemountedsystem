package com.example.vehicle_mountedsystem.ui;

import android.view.View;
import android.widget.TextView;
import com.example.vehicle_mountedsystem.R;
import java.util.HashMap;
import java.util.Map;

public class TabController {

    public interface OnTabSelectedListener {
        void onTabSelected(Tab tab);
    }

    public enum Tab {
        OVERVIEW(R.id.tabOverview, R.string.title_overview, R.string.placeholder_overview),
        MINIMAL(R.id.tabMinimal, R.string.title_minimal, R.string.placeholder_minimal),
        NAVIGATION(R.id.tabNavigation, R.string.title_navigation, R.string.placeholder_navigation),
        SENSORS(R.id.tabSensors, R.string.title_sensors, R.string.placeholder_sensors),
        HVAC(R.id.tabHvac, R.string.title_hvac, R.string.placeholder_hvac),
        MEDIA(R.id.tabMedia, R.string.title_media, R.string.placeholder_media),
        CONTROLS(R.id.tabControls, R.string.title_controls, R.string.placeholder_controls),
        SETTINGS(R.id.tabSettings, R.string.title_settings, R.string.placeholder_settings);

        private final int viewId;
        private final int titleResId;
        private final int placeholderResId;

        Tab(int viewId, int titleResId, int placeholderResId) {
            this.viewId = viewId;
            this.titleResId = titleResId;
            this.placeholderResId = placeholderResId;
        }

        public int getViewId() {
            return viewId;
        }

        public int getTitleResId() {
            return titleResId;
        }

        public int getPlaceholderResId() {
            return placeholderResId;
        }
    }

    private final View root;
    private final OnTabSelectedListener listener;
    private final Map<Tab, TextView> tabViews = new HashMap<>();
    private Tab currentTab;

    public TabController(View root, OnTabSelectedListener listener) {
        this.root = root;
        this.listener = listener;
        initTabs();
    }

    private void initTabs() {
        for (Tab tab : Tab.values()) {
            TextView tabView = root.findViewById(tab.getViewId());
            if (tabView != null) {
                tabViews.put(tab, tabView);
                tabView.setOnClickListener(v -> selectTab(tab));
            }
        }
    }

    public void selectTab(Tab tab) {
        if (currentTab == tab) {
            return;
        }
        currentTab = tab;
        updateTabVisuals();
        if (listener != null) {
            listener.onTabSelected(tab);
        }
    }

    public Tab getCurrentTab() {
        return currentTab;
    }

    private void updateTabVisuals() {
        int selectedColor = root.getContext().getColor(R.color.accent_cyan);
        int unselectedColor = root.getContext().getColor(R.color.text_secondary);

        for (Map.Entry<Tab, TextView> entry : tabViews.entrySet()) {
            Tab tab = entry.getKey();
            TextView view = entry.getValue();
            if (tab == currentTab) {
                view.setTextColor(selectedColor);
                view.setBackgroundResource(R.drawable.bg_tab_selected);
                view.setSelected(true);
            } else {
                view.setTextColor(unselectedColor);
                view.setBackgroundResource(0);
                view.setSelected(false);
            }
        }
    }
}
