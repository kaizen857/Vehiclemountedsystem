package com.example.vehicle_mountedsystem.util;

import android.view.View;

public final class AnimationHelper {
    private static final long PAGE_ENTER_DURATION_MILLIS = 180L;
    private static final long STATE_FEEDBACK_DURATION_MILLIS = 140L;

    private AnimationHelper() {
    }

    public static void playPageEnter(View view) {
        if (view == null) {
            return;
        }
        view.setAlpha(0.0f);
        view.setTranslationY(12.0f);
        view.animate()
                .alpha(1.0f)
                .translationY(0.0f)
                .setDuration(PAGE_ENTER_DURATION_MILLIS)
                .start();
    }

    public static void playStateFeedback(View view) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        view.setScaleX(0.96f);
        view.setScaleY(0.96f);
        view.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(STATE_FEEDBACK_DURATION_MILLIS)
                .start();
    }
}
