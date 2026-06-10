package com.example.vehicle_mountedsystem.model;

import java.util.Objects;

public final class MediaState {
    private final String title;
    private final String artist;
    private final boolean playing;
    private final AvailabilityStatus availabilityStatus;

    public MediaState(String title, String artist, boolean playing, AvailabilityStatus availabilityStatus) {
        this.title = requireText(title, "title");
        this.artist = requireText(artist, "artist");
        this.playing = playing;
        this.availabilityStatus = Objects.requireNonNull(availabilityStatus, "availabilityStatus");
    }

    public static MediaState defaultState() {
        return new MediaState("无媒体", "未知艺术家", false, AvailabilityStatus.unavailable());
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

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MediaState)) {
            return false;
        }
        MediaState that = (MediaState) other;
        return playing == that.playing
                && title.equals(that.title)
                && artist.equals(that.artist)
                && availabilityStatus.equals(that.availabilityStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, artist, playing, availabilityStatus);
    }
}
