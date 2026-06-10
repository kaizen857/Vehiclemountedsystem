package com.example.vehicle_mountedsystem.ui;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.vehicle_mountedsystem.R;

public class MainShellController {

    private final View root;
    private final TextView pageTitleView;
    private final FrameLayout pageHostView;
    private final TabController tabController;

    public MainShellController(View root) {
        this.root = root;
        this.pageTitleView = root.findViewById(R.id.pageTitle);
        this.pageHostView = root.findViewById(R.id.pageHost);
        this.tabController = new TabController(root, this::handleTabSelection);
        this.tabController.selectTab(TabController.Tab.OVERVIEW);
    }

    private void handleTabSelection(TabController.Tab tab) {
        if (pageTitleView != null) {
            pageTitleView.setText(tab.getTitleResId());
        }
        
        if (pageHostView != null) {
            pageHostView.removeAllViews();
            
            TextView placeholder = new TextView(root.getContext());
            placeholder.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
            ));
            placeholder.setGravity(Gravity.CENTER);
            placeholder.setText(tab.getPlaceholderResId());
            placeholder.setTextColor(root.getContext().getColor(R.color.text_secondary));
            placeholder.setTextSize(
                    android.util.TypedValue.COMPLEX_UNIT_PX,
                    root.getResources().getDimension(R.dimen.shell_placeholder_text_size)
            );
            
            pageHostView.addView(placeholder);
        }
    }

    public TabController getTabController() {
        return tabController;
    }
}
