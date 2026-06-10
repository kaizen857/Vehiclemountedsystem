package com.example.vehicle_mountedsystem;

import com.example.vehicle_mountedsystem.data.media.SystemMediaController;
import com.example.vehicle_mountedsystem.data.media.SystemMediaController.ConnectionMode;
import com.example.vehicle_mountedsystem.data.media.SystemMediaController.MediaSnapshot;
import com.example.vehicle_mountedsystem.data.media.SystemMediaController.SessionGateway;
import com.example.vehicle_mountedsystem.data.media.SystemMediaController.SessionSnapshot;
import com.example.vehicle_mountedsystem.data.media.SystemMediaController.TransportAction;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SystemMediaControllerTest {
    @Test
    public void loadSnapshot_withActiveSessionExposesMetadataAndPlayback() {
        FakeSessionGateway sessionGateway = new FakeSessionGateway(true, new SessionSnapshot("夜间巡航", "车载电台", true));
        FakeMediaKeyGateway mediaKeyGateway = new FakeMediaKeyGateway();
        SystemMediaController controller = new SystemMediaController(sessionGateway, mediaKeyGateway);

        MediaSnapshot snapshot = controller.loadSnapshot();

        assertEquals(ConnectionMode.ACTIVE_SESSION, snapshot.getConnectionMode());
        assertEquals("夜间巡航", snapshot.getMediaState().getTitle());
        assertEquals("车载电台", snapshot.getMediaState().getArtist());
        assertTrue(snapshot.getMediaState().isPlaying());
        assertTrue(snapshot.getMediaState().getAvailabilityStatus().isAvailable());
        assertEquals("已连接系统媒体会话，可读取曲目信息并使用传输控制。", snapshot.getMessage());
    }

    @Test
    public void dispatch_withActiveSessionUsesSessionControls() {
        FakeSessionGateway sessionGateway = new FakeSessionGateway(true, new SessionSnapshot("曲目", "艺术家", false));
        FakeMediaKeyGateway mediaKeyGateway = new FakeMediaKeyGateway();
        SystemMediaController controller = new SystemMediaController(sessionGateway, mediaKeyGateway);

        controller.dispatch(TransportAction.NEXT);

        assertEquals(1, sessionGateway.actions.size());
        assertEquals(TransportAction.NEXT, sessionGateway.actions.get(0));
        assertTrue(mediaKeyGateway.actions.isEmpty());
    }

    @Test
    public void dispatch_withoutPermissionFallsBackToMediaKey() {
        FakeSessionGateway sessionGateway = new FakeSessionGateway(false, new SessionSnapshot("曲目", "艺术家", true));
        FakeMediaKeyGateway mediaKeyGateway = new FakeMediaKeyGateway();
        SystemMediaController controller = new SystemMediaController(sessionGateway, mediaKeyGateway);

        MediaSnapshot snapshot = controller.dispatch(TransportAction.PLAY_PAUSE);

        assertEquals(ConnectionMode.MEDIA_KEY_FALLBACK, snapshot.getConnectionMode());
        assertFalse(snapshot.getMediaState().getAvailabilityStatus().isAvailable());
        assertEquals("无媒体", snapshot.getMediaState().getTitle());
        assertEquals(1, mediaKeyGateway.actions.size());
        assertEquals(TransportAction.PLAY_PAUSE, mediaKeyGateway.actions.get(0));
        assertTrue(sessionGateway.actions.isEmpty());
    }

    @Test
    public void dispatch_withoutActiveSessionFallsBackToMediaKey() {
        FakeSessionGateway sessionGateway = new FakeSessionGateway(true, null);
        FakeMediaKeyGateway mediaKeyGateway = new FakeMediaKeyGateway();
        SystemMediaController controller = new SystemMediaController(sessionGateway, mediaKeyGateway);

        MediaSnapshot snapshot = controller.dispatch(TransportAction.PREVIOUS);

        assertEquals(ConnectionMode.NO_ACTIVE_SESSION, snapshot.getConnectionMode());
        assertEquals("未发现活动媒体会话，请先打开音乐应用；控制按钮将降级发送系统媒体键。", snapshot.getMessage());
        assertEquals(1, mediaKeyGateway.actions.size());
        assertEquals(TransportAction.PREVIOUS, mediaKeyGateway.actions.get(0));
        assertTrue(sessionGateway.actions.isEmpty());
    }

    private static final class FakeSessionGateway implements SessionGateway {
        private final boolean notificationAccess;
        private final SessionSnapshot sessionSnapshot;
        private final List<TransportAction> actions = new ArrayList<>();

        private FakeSessionGateway(boolean notificationAccess, SessionSnapshot sessionSnapshot) {
            this.notificationAccess = notificationAccess;
            this.sessionSnapshot = sessionSnapshot;
        }

        @Override
        public boolean hasNotificationAccess() {
            return notificationAccess;
        }

        @Override
        public SessionSnapshot getActiveSession() {
            return sessionSnapshot;
        }

        @Override
        public void dispatch(TransportAction action) {
            actions.add(action);
        }
    }

    private static final class FakeMediaKeyGateway implements SystemMediaController.MediaKeyGateway {
        private final List<TransportAction> actions = new ArrayList<>();

        @Override
        public void dispatch(TransportAction action) {
            actions.add(action);
        }
    }
}
