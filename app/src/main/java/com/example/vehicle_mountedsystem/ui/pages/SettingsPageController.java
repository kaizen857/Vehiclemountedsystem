package com.example.vehicle_mountedsystem.ui.pages;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.vehicle_mountedsystem.R;
import com.example.vehicle_mountedsystem.util.AnimationHelper;

public final class SettingsPageController {

    public View createView(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_settings, parent, false);
        bind(view);
        AnimationHelper.playPageEnter(view);
        return view;
    }

    private void bind(View view) {
        Context context = view.getContext();
        text(view, R.id.settingNotificationStatus, notificationStatus(context));
        text(view, R.id.settingSensorStatus, sensorStatus(context));
        text(view, R.id.settingIrStatus, context.getString(R.string.settings_ir_local_only));
        text(view, R.id.settingMediaStatus, mediaStatus(context));
        text(view, R.id.settingDataSource, context.getString(R.string.settings_data_source_value));
    }

    private static String notificationStatus(Context context) {
        String enabledListeners = Settings.Secure.getString(
                context.getContentResolver(),
                "enabled_notification_listeners");
        if (enabledListeners != null && enabledListeners.contains(context.getPackageName())) {
            return "通知监听已授权：可读取系统媒体会话摘要。";
        }
        return "通知监听未授权：媒体页保留本地降级说明，不读取通知内容。";
    }

    private static String sensorStatus(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        boolean accelerometer = hasSensor(sensorManager, Sensor.TYPE_ACCELEROMETER);
        boolean gyroscope = hasSensor(sensorManager, Sensor.TYPE_GYROSCOPE);
        return "传感器能力：加速度计" + label(accelerometer)
                + " · 陀螺仪" + label(gyroscope)
                + "；演示页缺省使用安全回退文案。";
    }

    private static String mediaStatus(Context context) {
        PackageManager packageManager = context.getPackageManager();
        boolean audioLowLatency = packageManager != null
                && packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY);
        return "媒体会话：已声明通知监听服务；当前音频低延迟能力"
                + label(audioLowLatency)
                + "，未授权时仅显示降级状态。";
    }

    private static boolean hasSensor(SensorManager sensorManager, int sensorType) {
        return sensorManager != null && sensorManager.getDefaultSensor(sensorType) != null;
    }

    private static String label(boolean available) {
        return available ? "可用" : "不可用";
    }

    private static void text(View view, int id, String value) {
        TextView textView = view.findViewById(id);
        textView.setText(value);
    }
}
