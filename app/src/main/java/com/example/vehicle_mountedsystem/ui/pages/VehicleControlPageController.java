package com.example.vehicle_mountedsystem.ui.pages;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.vehicle_mountedsystem.R;
import com.example.vehicle_mountedsystem.util.AnimationHelper;

public final class VehicleControlPageController {
    public static final String PREFERENCES_NAME = "vehicle_control_demo_state";

    private static final String KEY_WINDOW_FRONT_LEFT_OPEN = "window_front_left_open";
    private static final String KEY_MIRROR_LEFT_FOLDED = "mirror_left_folded";
    private static final String KEY_SEAT_HEAT_ENABLED = "seat_heat_enabled";
    private static final String KEY_LOCKED = "locked";

    private SharedPreferences preferences;
    private TextView windowFrontLeftState;
    private TextView mirrorLeftState;
    private TextView seatHeatState;
    private TextView lockState;
    private TextView demoSummary;

    public View createView(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_control, parent, false);
        preferences = parent.getContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        bindViews(view);
        bindActions(view);
        render();
        AnimationHelper.playPageEnter(view);
        return view;
    }

    private void bindViews(View view) {
        windowFrontLeftState = view.findViewById(R.id.windowFrontLeftState);
        mirrorLeftState = view.findViewById(R.id.mirrorLeftState);
        seatHeatState = view.findViewById(R.id.seatHeatState);
        lockState = view.findViewById(R.id.lockDemoState);
        demoSummary = view.findViewById(R.id.controlDemoSummary);
    }

    private void bindActions(View view) {
        click(view, R.id.windowFrontLeftAction, () -> toggle(KEY_WINDOW_FRONT_LEFT_OPEN, windowFrontLeftState));
        click(view, R.id.mirrorLeftAction, () -> toggle(KEY_MIRROR_LEFT_FOLDED, mirrorLeftState));
        click(view, R.id.seatHeatAction, () -> toggle(KEY_SEAT_HEAT_ENABLED, seatHeatState));
        click(view, R.id.lockDemoAction, () -> toggle(KEY_LOCKED, lockState));
    }

    private void toggle(String key, View feedbackView) {
        preferences.edit().putBoolean(key, !preferences.getBoolean(key, false)).apply();
        render();
        AnimationHelper.playStateFeedback(feedbackView);
    }

    private void render() {
        boolean windowOpen = preferences.getBoolean(KEY_WINDOW_FRONT_LEFT_OPEN, false);
        boolean mirrorFolded = preferences.getBoolean(KEY_MIRROR_LEFT_FOLDED, false);
        boolean seatHeatEnabled = preferences.getBoolean(KEY_SEAT_HEAT_ENABLED, false);
        boolean locked = preferences.getBoolean(KEY_LOCKED, false);

        windowFrontLeftState.setText(windowOpen ? "本地演示：左前窗已打开" : "本地演示：左前窗已关闭");
        mirrorLeftState.setText(mirrorFolded ? "本地演示：左后视镜已折叠" : "本地演示：左后视镜已展开");
        seatHeatState.setText(seatHeatEnabled ? "本地演示：座椅加热已开启" : "本地演示：座椅加热已关闭");
        lockState.setText(locked ? "本地演示：车门已上锁" : "本地演示：车门已解锁");
        demoSummary.setText(summary(windowOpen, mirrorFolded, seatHeatEnabled, locked));
    }

    private static String summary(boolean windowOpen, boolean mirrorFolded, boolean seatHeatEnabled, boolean locked) {
        return "演示状态：车窗" + (windowOpen ? "打开" : "关闭")
                + " · 后视镜" + (mirrorFolded ? "折叠" : "展开")
                + " · 座椅加热" + (seatHeatEnabled ? "开启" : "关闭")
                + " · 车门" + (locked ? "上锁" : "解锁");
    }

    private static void click(View root, int id, Action action) {
        root.findViewById(id).setOnClickListener(v -> action.run());
    }

    private interface Action {
        void run();
    }
}
