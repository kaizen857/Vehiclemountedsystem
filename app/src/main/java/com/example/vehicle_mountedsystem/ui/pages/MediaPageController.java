package com.example.vehicle_mountedsystem.ui.pages;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.vehicle_mountedsystem.R;
import com.example.vehicle_mountedsystem.data.media.SystemMediaController;
import com.example.vehicle_mountedsystem.model.MediaState;
import com.example.vehicle_mountedsystem.util.AnimationHelper;

public final class MediaPageController {
    private final SystemMediaController mediaController;

    private TextView statusMessage;
    private TextView titleValue;
    private TextView artistValue;
    private TextView playbackValue;
    private TextView connectionModeValue;

    public MediaPageController(SystemMediaController mediaController) {
        this.mediaController = mediaController;
    }

    public View createView(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_media, parent, false);
        bindViews(view);
        mediaController.setSnapshotListener(snapshot -> view.post(() -> render(snapshot)));
        bindActions(view);
        render(mediaController.loadSnapshot());
        AnimationHelper.playPageEnter(view);
        return view;
    }

    public void stop() {
        mediaController.setSnapshotListener(null);
    }

    private void bindViews(View view) {
        statusMessage = view.findViewById(R.id.mediaStatusMessage);
        titleValue = view.findViewById(R.id.mediaTitleValue);
        artistValue = view.findViewById(R.id.mediaArtistValue);
        playbackValue = view.findViewById(R.id.mediaPlaybackValue);
        connectionModeValue = view.findViewById(R.id.mediaConnectionModeValue);
    }

    private void bindActions(View view) {
        click(view, R.id.mediaPreviousControl, () -> dispatchAndRefresh(view, SystemMediaController.TransportAction.PREVIOUS));
        click(view, R.id.mediaPlayPauseControl, () -> dispatchAndRefresh(view, SystemMediaController.TransportAction.PLAY_PAUSE));
        click(view, R.id.mediaNextControl, () -> dispatchAndRefresh(view, SystemMediaController.TransportAction.NEXT));
        click(view, R.id.mediaPermissionAction, () -> openNotificationSettings(view.getContext()));
    }

    private void dispatchAndRefresh(View view, SystemMediaController.TransportAction action) {
        render(mediaController.dispatch(action));
        view.postDelayed(() -> render(mediaController.loadSnapshot()), 300L);
    }

    private void render(SystemMediaController.MediaSnapshot snapshot) {
        MediaState mediaState = snapshot.getMediaState();
        statusMessage.setText(snapshot.getMessage());
        titleValue.setText(mediaState.getTitle());
        artistValue.setText(mediaState.getArtist());
        playbackValue.setText(mediaState.isPlaying() ? "正在播放" : "已暂停/未播放");
        connectionModeValue.setText(labelFor(snapshot.getConnectionMode()));
    }

    private static String labelFor(SystemMediaController.ConnectionMode connectionMode) {
        if (connectionMode == SystemMediaController.ConnectionMode.ACTIVE_SESSION) {
            return "已连接媒体会话";
        }
        if (connectionMode == SystemMediaController.ConnectionMode.MEDIA_KEY_FALLBACK) {
            return "媒体键降级模式";
        }
        return "无活动媒体会话";
    }

    private static void openNotificationSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            context.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    private static void click(View root, int id, Action action) {
        root.findViewById(id).setOnClickListener(v -> action.run());
    }

    private interface Action {
        void run();
    }
}
