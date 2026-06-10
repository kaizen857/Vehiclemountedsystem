package com.example.vehicle_mountedsystem.data.media;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.provider.Settings;
import android.view.KeyEvent;

import com.example.vehicle_mountedsystem.model.AvailabilityStatus;
import com.example.vehicle_mountedsystem.model.MediaState;

import java.util.List;
import java.util.Objects;

public final class SystemMediaController {
    private static final String UNKNOWN_TITLE = "无标题媒体";
    private static final String UNKNOWN_ARTIST = "未知艺术家";
    private static final long TIMESTAMP_MILLIS = 0L;

    private final SessionGateway sessionGateway;
    private final MediaKeyGateway mediaKeyGateway;

    public SystemMediaController(Context context) {
        Context appContext = context.getApplicationContext();
        this.sessionGateway = new AndroidSessionGateway(appContext);
        this.mediaKeyGateway = new AndroidMediaKeyGateway(appContext);
    }

    public SystemMediaController(SessionGateway sessionGateway, MediaKeyGateway mediaKeyGateway) {
        this.sessionGateway = Objects.requireNonNull(sessionGateway, "sessionGateway");
        this.mediaKeyGateway = Objects.requireNonNull(mediaKeyGateway, "mediaKeyGateway");
    }

    public MediaSnapshot loadSnapshot() {
        if (!sessionGateway.hasNotificationAccess()) {
            return new MediaSnapshot(
                    MediaState.defaultState(),
                    ConnectionMode.MEDIA_KEY_FALLBACK,
                    "未授权通知使用权，无法读取媒体会话；控制按钮将降级发送系统媒体键。");
        }

        SessionSnapshot session = sessionGateway.getActiveSession();
        if (session == null) {
            return new MediaSnapshot(
                    MediaState.defaultState(),
                    ConnectionMode.NO_ACTIVE_SESSION,
                    "未发现活动媒体会话，请先打开音乐应用；控制按钮将降级发送系统媒体键。");
        }

        return new MediaSnapshot(
                new MediaState(
                        textOrDefault(session.getTitle(), UNKNOWN_TITLE),
                        textOrDefault(session.getArtist(), UNKNOWN_ARTIST),
                        session.isPlaying(),
                        AvailabilityStatus.available("已连接系统媒体会话", TIMESTAMP_MILLIS)),
                ConnectionMode.ACTIVE_SESSION,
                "已连接系统媒体会话，可读取曲目信息并使用传输控制。");
    }

    public MediaSnapshot dispatch(TransportAction action) {
        Objects.requireNonNull(action, "action");
        MediaSnapshot snapshot = loadSnapshot();
        if (snapshot.getConnectionMode() == ConnectionMode.ACTIVE_SESSION) {
            sessionGateway.dispatch(action);
        } else {
            mediaKeyGateway.dispatch(action);
        }
        return loadSnapshot();
    }

    private static String textOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    public enum ConnectionMode {
        ACTIVE_SESSION,
        MEDIA_KEY_FALLBACK,
        NO_ACTIVE_SESSION
    }

    public enum TransportAction {
        PLAY_PAUSE,
        PREVIOUS,
        NEXT
    }

    public interface SessionGateway {
        boolean hasNotificationAccess();

        SessionSnapshot getActiveSession();

        void dispatch(TransportAction action);
    }

    public interface MediaKeyGateway {
        void dispatch(TransportAction action);
    }

    public static final class SessionSnapshot {
        private final String title;
        private final String artist;
        private final boolean playing;

        public SessionSnapshot(String title, String artist, boolean playing) {
            this.title = title;
            this.artist = artist;
            this.playing = playing;
        }

        public String getTitle() {
            return title;
        }

        public String getArtist() {
            return artist;
        }

        public boolean isPlaying() {
            return playing;
        }
    }

    public static final class MediaSnapshot {
        private final MediaState mediaState;
        private final ConnectionMode connectionMode;
        private final String message;

        private MediaSnapshot(MediaState mediaState, ConnectionMode connectionMode, String message) {
            this.mediaState = Objects.requireNonNull(mediaState, "mediaState");
            this.connectionMode = Objects.requireNonNull(connectionMode, "connectionMode");
            this.message = textOrDefault(message, "媒体状态不可用");
        }

        public MediaState getMediaState() {
            return mediaState;
        }

        public ConnectionMode getConnectionMode() {
            return connectionMode;
        }

        public String getMessage() {
            return message;
        }
    }

    private static final class AndroidSessionGateway implements SessionGateway {
        private final Context context;
        private final MediaSessionManager sessionManager;
        private MediaController activeController;

        private AndroidSessionGateway(Context context) {
            this.context = context;
            this.sessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        }

        @Override
        public boolean hasNotificationAccess() {
            String enabledListeners = Settings.Secure.getString(
                    context.getContentResolver(),
                    "enabled_notification_listeners");
            String packageName = context.getPackageName();
            return enabledListeners != null && enabledListeners.contains(packageName);
        }

        @Override
        public SessionSnapshot getActiveSession() {
            if (!hasNotificationAccess() || sessionManager == null) {
                activeController = null;
                return null;
            }
            try {
                List<MediaController> controllers = sessionManager.getActiveSessions(new ComponentName(
                        context,
                        MediaNotificationListenerService.class));
                activeController = firstController(controllers);
                if (activeController == null) {
                    return null;
                }
                MediaMetadata metadata = activeController.getMetadata();
                PlaybackState playbackState = activeController.getPlaybackState();
                String title = metadata == null ? UNKNOWN_TITLE : metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                String artist = metadata == null ? UNKNOWN_ARTIST : metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                boolean playing = playbackState != null
                        && playbackState.getState() == PlaybackState.STATE_PLAYING;
                return new SessionSnapshot(title, artist, playing);
            } catch (SecurityException ignored) {
                activeController = null;
                return null;
            }
        }

        @Override
        public void dispatch(TransportAction action) {
            MediaController controller = activeController;
            if (controller == null) {
                getActiveSession();
                controller = activeController;
            }
            if (controller == null) {
                return;
            }
            MediaController.TransportControls controls = controller.getTransportControls();
            if (action == TransportAction.NEXT) {
                controls.skipToNext();
            } else if (action == TransportAction.PREVIOUS) {
                controls.skipToPrevious();
            } else {
                PlaybackState playbackState = controller.getPlaybackState();
                if (playbackState != null && playbackState.getState() == PlaybackState.STATE_PLAYING) {
                    controls.pause();
                } else {
                    controls.play();
                }
            }
        }

        private static MediaController firstController(List<MediaController> controllers) {
            if (controllers == null || controllers.isEmpty()) {
                return null;
            }
            return controllers.get(0);
        }
    }

    private static final class AndroidMediaKeyGateway implements MediaKeyGateway {
        private final AudioManager audioManager;

        private AndroidMediaKeyGateway(Context context) {
            this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        @Override
        public void dispatch(TransportAction action) {
            if (audioManager == null) {
                return;
            }
            int keyCode = keyCodeFor(action);
            audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        }

        private static int keyCodeFor(TransportAction action) {
            if (action == TransportAction.NEXT) {
                return KeyEvent.KEYCODE_MEDIA_NEXT;
            }
            if (action == TransportAction.PREVIOUS) {
                return KeyEvent.KEYCODE_MEDIA_PREVIOUS;
            }
            return KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
        }
    }
}
