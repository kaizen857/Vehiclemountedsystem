package com.example.vehicle_mountedsystem.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.View;

public final class AttitudeIndicatorView extends View {

    private static final int SKY_COLOR = Color.rgb(20, 80, 160);
    private static final int GROUND_COLOR = Color.rgb(80, 40, 20);
    private static final int HORIZON_LINE_COLOR = Color.rgb(200, 220, 240);
    private static final int SCALE_MARK_COLOR = Color.rgb(180, 200, 220);
    private static final int CENTER_MARKER_COLOR = Color.rgb(6, 182, 212);
    private static final int ROLL_ARC_COLOR = Color.rgb(148, 163, 184);
    private static final int ROLL_POINTER_COLOR = Color.rgb(6, 182, 212);
    private static final int BACKGROUND_COLOR = Color.rgb(15, 23, 42);
    private static final int TEXT_COLOR = Color.rgb(248, 250, 252);

    private float gyroPitchDegrees;
    private float gyroRollDegrees;
    private float filteredPitchDegrees;
    private float filteredRollDegrees;
    private float pitchOffset;
    private float rollOffset;
    private int pitchSign = 1;
    private int rollSign = 1;
    private long lastGyroTimestampMillis = -1L;
    private String dataSourceLabel = "同步中...";

    private final Paint skyPaint;
    private final Paint groundPaint;
    private final Paint horizonPaint;
    private final Paint scalePaint;
    private final Paint centerMarkerPaint;
    private final Paint rollArcPaint;
    private final Paint rollPointerPaint;
    private final Paint textPaint;
    private final Paint bgPaint;
    private final Path clipPath;
    private final Path ptrPath;
    private final Path topPtrPath;
    private final RectF clipOval;
    private final RectF rollArcRect;
    private final float density;

    public AttitudeIndicatorView(Context context) {
        this(context, null);
    }

    public AttitudeIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;

        skyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        skyPaint.setColor(SKY_COLOR);
        skyPaint.setStyle(Paint.Style.FILL);

        groundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        groundPaint.setColor(GROUND_COLOR);
        groundPaint.setStyle(Paint.Style.FILL);

        horizonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        horizonPaint.setColor(HORIZON_LINE_COLOR);
        horizonPaint.setStyle(Paint.Style.STROKE);
        horizonPaint.setStrokeWidth(dp(2f));

        scalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scalePaint.setColor(SCALE_MARK_COLOR);
        scalePaint.setStyle(Paint.Style.STROKE);
        scalePaint.setStrokeWidth(dp(1.5f));

        centerMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerMarkerPaint.setColor(CENTER_MARKER_COLOR);
        centerMarkerPaint.setStyle(Paint.Style.STROKE);
        centerMarkerPaint.setStrokeWidth(dp(2.5f));

        rollArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rollArcPaint.setColor(ROLL_ARC_COLOR);
        rollArcPaint.setStyle(Paint.Style.STROKE);
        rollArcPaint.setStrokeWidth(dp(2f));

        rollPointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rollPointerPaint.setColor(ROLL_POINTER_COLOR);
        rollPointerPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(sp(10f));
        textPaint.setTextAlign(Paint.Align.CENTER);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(BACKGROUND_COLOR);
        bgPaint.setStyle(Paint.Style.FILL);

        clipPath = new Path();
        ptrPath = new Path();
        topPtrPath = new Path();
        clipOval = new RectF();
        rollArcRect = new RectF();
    }

    public void updateGyroscope(float gyroX, float gyroY, float gyroZ, long timestampMillis) {
        if (lastGyroTimestampMillis < 0L) {
            lastGyroTimestampMillis = timestampMillis;
            return;
        }
        long deltaMs = timestampMillis - lastGyroTimestampMillis;
        if (deltaMs <= 0L || deltaMs > 500L) {
            lastGyroTimestampMillis = timestampMillis;
            return;
        }
        float dtSeconds = deltaMs / 1000f;

        gyroRollDegrees += (float) Math.toDegrees(gyroX) * dtSeconds;
        gyroPitchDegrees += (float) Math.toDegrees(gyroY) * dtSeconds;

        gyroRollDegrees = Math.max(-90f, Math.min(90f, gyroRollDegrees));
        gyroPitchDegrees = Math.max(-90f, Math.min(90f, gyroPitchDegrees));

        filteredPitchDegrees = 0.98f * filteredPitchDegrees + 0.02f * gyroPitchDegrees;
        filteredRollDegrees = 0.98f * filteredRollDegrees + 0.02f * gyroRollDegrees;

        dataSourceLabel = "传感器";
        lastGyroTimestampMillis = timestampMillis;
        postInvalidate();
    }

    public void updateFromRotationVector(float[] rotationVector) {
        // Samsung bug workaround: some devices return values.length > 4,
        // which crashes getRotationMatrixFromVector. Truncate to first 4.
        // (crbug.com/335298, used by Telegram/OneBusAway/MD360Player/ExoPlayer)
        float[] truncated = rotationVector;
        if (rotationVector.length > 4) {
            truncated = new float[]{rotationVector[0], rotationVector[1], rotationVector[2], rotationVector[3]};
        }

        float[] rotMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotMatrix, truncated);
        float[] remapped = new float[9];
        SensorManager.remapCoordinateSystem(rotMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, remapped);
        float[] orientation = new float[3];
        SensorManager.getOrientation(remapped, orientation);

        filteredPitchDegrees = (float) Math.toDegrees(orientation[1]);
        filteredRollDegrees = (float) Math.toDegrees(orientation[2]);

        dataSourceLabel = "姿态融合";
        postInvalidate();
    }

    public void calibrateFromAccelerometer(float accelX, float accelY, float accelZ) {
        float gravity = (float) Math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);
        if (gravity < 0.1f) return;
        float ax = accelX / gravity;
        float ay = accelY / gravity;
        float az = accelZ / gravity;

        float accelPitch = (float) Math.toDegrees(Math.asin(Math.max(-1f, Math.min(1f, ax))));
        float accelRoll = (float) Math.toDegrees(Math.atan2(ay, Math.max(0.01f, az)));

        filteredPitchDegrees = accelPitch;
        filteredRollDegrees = accelRoll;
        lastGyroTimestampMillis = -1L;
        dataSourceLabel = "校准";
        postInvalidate();
    }

    public void reset() {
        gyroPitchDegrees = 0f;
        gyroRollDegrees = 0f;
        filteredPitchDegrees = 0f;
        filteredRollDegrees = 0f;
        pitchOffset = 0f;
        rollOffset = 0f;
        lastGyroTimestampMillis = -1L;
        dataSourceLabel = "已重设";
        postInvalidate();
    }

    public void zeroCurrentAttitude() {
        pitchOffset = filteredPitchDegrees;
        rollOffset = filteredRollDegrees;
        dataSourceLabel = "已校准";
        postInvalidate();
    }

    public void flipPitch() {
        pitchSign = -pitchSign;
        postInvalidate();
    }

    public void flipRoll() {
        rollSign = -rollSign;
        postInvalidate();
    }

    public boolean isPitchFlipped() {
        return pitchSign < 0;
    }

    public boolean isRollFlipped() {
        return rollSign < 0;
    }

    public float getPitchDegrees() {
        return filteredPitchDegrees;
    }

    public float getRollDegrees() {
        return filteredRollDegrees;
    }

    public String getDataSourceLabel() {
        return dataSourceLabel;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(cx, cy) - dp(12f);

        clipOval.set(cx - radius, cy - radius, cx + radius, cy + radius);
        clipPath.reset();
        clipPath.addOval(clipOval, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clipPath);
        canvas.drawOval(clipOval, bgPaint);

        canvas.save();
        float displayRoll = rollSign * (filteredRollDegrees - rollOffset);
        float displayPitch = pitchSign * (filteredPitchDegrees - pitchOffset);
        canvas.rotate(-displayRoll, cx, cy);

        float pitchScale = radius * 1.5f / 30f;
        float pitchOffsetPx = -displayPitch * pitchScale;
        float horizonY = cy + pitchOffsetPx;

        skyPaint.setColor(SKY_COLOR);
        canvas.drawRect(0, 0, w, horizonY, skyPaint);
        groundPaint.setColor(GROUND_COLOR);
        canvas.drawRect(0, horizonY, w, h + radius, groundPaint);
        canvas.drawLine(0, horizonY, w, horizonY, horizonPaint);

        for (int deg = -60; deg <= 60; deg += 10) {
            if (deg == 0) continue;
            float markY = cy + (-deg * pitchScale);
            float markLen = (deg % 30 == 0) ? dp(24f) : dp(14f);
            canvas.drawLine(cx - markLen, markY, cx + markLen, markY, scalePaint);
            if (deg % 30 == 0) {
                canvas.drawText(deg + "°", cx + markLen + dp(18f), markY + sp(4f), textPaint);
            }
        }

        canvas.restore();

        float markerWingSpan = dp(36f);
        float markerBodyLen = dp(20f);
        float markerGap = dp(6f);
        canvas.drawLine(cx - markerWingSpan, cy, cx - markerGap, cy, centerMarkerPaint);
        canvas.drawLine(cx + markerGap, cy, cx + markerWingSpan, cy, centerMarkerPaint);
        centerMarkerPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, dp(3f), centerMarkerPaint);
        centerMarkerPaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(cx, cy - markerBodyLen, cx, cy - dp(4f), centerMarkerPaint);
        canvas.drawLine(cx, cy + dp(4f), cx, cy + markerBodyLen, centerMarkerPaint);

        canvas.restore();

        float arcRadius = radius + dp(6f);
        rollArcRect.set(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius);
        canvas.drawArc(rollArcRect, 210f, 120f, false, rollArcPaint);

        for (int deg = -60; deg <= 60; deg += 30) {
            float angleRad = (float) Math.toRadians(deg - 90);
            float innerR = arcRadius - dp(4f);
            float outerR = arcRadius + dp(6f);
            canvas.drawLine(
                    cx + innerR * (float) Math.cos(angleRad),
                    cy + innerR * (float) Math.sin(angleRad),
                    cx + outerR * (float) Math.cos(angleRad),
                    cy + outerR * (float) Math.sin(angleRad),
                    scalePaint);
        }

        float pointerAngle = (float) Math.toRadians(-displayRoll - 90);
        float pointerR = arcRadius;
        float px = cx + pointerR * (float) Math.cos(pointerAngle);
        float py = cy + pointerR * (float) Math.sin(pointerAngle);
        float ptrSize = dp(7f);
        ptrPath.reset();
        float perpX = (float) Math.cos(pointerAngle + Math.PI / 2);
        float perpY = (float) Math.sin(pointerAngle + Math.PI / 2);
        ptrPath.moveTo(px + perpX * ptrSize, py + perpY * ptrSize);
        ptrPath.lineTo(px - perpX * ptrSize, py - perpY * ptrSize);
        ptrPath.lineTo(px - (float) Math.cos(pointerAngle) * ptrSize * 1.5f,
                py - (float) Math.sin(pointerAngle) * ptrSize * 1.5f);
        ptrPath.close();
        canvas.drawPath(ptrPath, rollPointerPaint);

        float topY = cy - arcRadius;
        topPtrPath.reset();
        topPtrPath.moveTo(cx, topY - dp(3f));
        topPtrPath.lineTo(cx - dp(6f), topY - dp(12f));
        topPtrPath.lineTo(cx + dp(6f), topY - dp(12f));
        topPtrPath.close();
        canvas.drawPath(topPtrPath, rollPointerPaint);

        String readout = String.format(java.util.Locale.US,
                "俯仰 %.1f°  滚转 %.1f°  [%s]", displayPitch, displayRoll, dataSourceLabel);
        canvas.drawText(readout, cx, cy + radius + dp(28f), textPaint);
    }

    private float dp(float dp) {
        return dp * density;
    }

    private float sp(float sp) {
        return sp * density;
    }
}
