package com.example.vehicle_mountedsystem.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public final class GForceIndicatorView extends View {

    private static final int BG_COLOR = Color.rgb(11, 15, 25);
    private static final int OUTER_RING_COLOR = Color.rgb(6, 182, 212);
    private static final int INNER_RING_COLOR = Color.rgb(30, 50, 70);
    private static final int CROSSHAIR_COLOR = Color.rgb(20, 35, 55);
    private static final int BALL_FILL_COLOR = Color.argb(200, 6, 182, 212);
    private static final int BALL_STROKE_COLOR = Color.rgb(6, 182, 212);
    private static final int TEXT_COLOR = Color.rgb(248, 250, 252);
    private static final int LABEL_COLOR = Color.rgb(148, 163, 184);
    private static final int TICK_COLOR = Color.rgb(40, 60, 80);
    private static final float MAX_G_RANGE = 1.0f;
    private static final float BALL_RADIUS_DP = 10f;
    private static final float OUTER_RING_WIDTH_DP = 2f;
    private static final float FILTER_ALPHA = 0.5f;

    private float gForceX;
    private float gForceY;
    private float gForceMagnitude;
    private float filteredBallX;
    private float filteredBallY;
    private float filteredMagnitude;
    private boolean hasData;
    private boolean ballCentered;

    private final Paint outerRingPaint;
    private final Paint innerRingPaint;
    private final Paint crosshairPaint;
    private final Paint ballFillPaint;
    private final Paint ballStrokePaint;
    private final Paint valueTextPaint;
    private final Paint labelTextPaint;
    private final Paint tickPaint;
    private final RectF outerRingRect;
    private final RectF innerRingRect;
    private final float density;
    private final float ballRadiusPx;

    public GForceIndicatorView(Context context) {
        this(context, null);
    }

    public GForceIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        ballRadiusPx = BALL_RADIUS_DP * density;

        outerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerRingPaint.setStyle(Paint.Style.STROKE);
        outerRingPaint.setStrokeWidth(OUTER_RING_WIDTH_DP * density);
        outerRingPaint.setColor(OUTER_RING_COLOR);

        innerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerRingPaint.setStyle(Paint.Style.STROKE);
        innerRingPaint.setStrokeWidth(1f * density);
        innerRingPaint.setColor(INNER_RING_COLOR);
        innerRingPaint.setPathEffect(new DashPathEffect(new float[]{6f * density, 8f * density}, 0f));

        crosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crosshairPaint.setStyle(Paint.Style.STROKE);
        crosshairPaint.setStrokeWidth(1f * density);
        crosshairPaint.setColor(CROSSHAIR_COLOR);

        ballFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ballFillPaint.setStyle(Paint.Style.FILL);
        ballFillPaint.setColor(BALL_FILL_COLOR);

        ballStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ballStrokePaint.setStyle(Paint.Style.STROKE);
        ballStrokePaint.setStrokeWidth(1.5f * density);
        ballStrokePaint.setColor(BALL_STROKE_COLOR);

        valueTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valueTextPaint.setColor(TEXT_COLOR);
        valueTextPaint.setTextAlign(Paint.Align.CENTER);
        valueTextPaint.setTextSize(28f * density);
        valueTextPaint.setFakeBoldText(true);

        labelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelTextPaint.setColor(LABEL_COLOR);
        labelTextPaint.setTextAlign(Paint.Align.CENTER);
        labelTextPaint.setTextSize(11f * density);

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(1f * density);
        tickPaint.setColor(TICK_COLOR);

        outerRingRect = new RectF();
        innerRingRect = new RectF();
    }

    public void updateGForce(float gx, float gy) {
        gForceX = gx;
        gForceY = gy;
        gForceMagnitude = (float) Math.sqrt(gx * gx + gy * gy);

        if (!ballCentered && getWidth() > 0) {
            filteredBallX = centerX();
            filteredBallY = centerY();
            ballCentered = true;
        }
        hasData = true;

        float targetBallX = centerX() + (gForceX / MAX_G_RANGE) * usableRadius();
        float targetBallY = centerY() - (gForceY / MAX_G_RANGE) * usableRadius();
        float clampedX = clampBallX(targetBallX);
        float clampedY = clampBallY(targetBallY);

        filteredBallX = filteredBallX * (1f - FILTER_ALPHA) + clampedX * FILTER_ALPHA;
        filteredBallY = filteredBallY * (1f - FILTER_ALPHA) + clampedY * FILTER_ALPHA;
        filteredMagnitude = filteredMagnitude * (1f - FILTER_ALPHA) + gForceMagnitude * FILTER_ALPHA;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cx = centerX();
        float cy = centerY();
        float radius = usableRadius();

        canvas.drawColor(BG_COLOR);

        drawTicks(canvas, cx, cy, radius);

        outerRingRect.set(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawOval(outerRingRect, outerRingPaint);

        float innerR = radius * 0.78f;
        innerRingRect.set(cx - innerR, cy - innerR, cx + innerR, cy + innerR);
        canvas.drawOval(innerRingRect, innerRingPaint);

        canvas.drawLine(cx - radius, cy, cx + radius, cy, crosshairPaint);
        canvas.drawLine(cx, cy - radius, cx, cy + radius, crosshairPaint);

        drawAxisLabels(canvas, cx, cy, radius);

        if (hasData) {
            drawBall(canvas, filteredBallX, filteredBallY);
            drawCenterValue(canvas, cx, cy);
        } else {
            canvas.drawText("同步中...", cx, cy, valueTextPaint);
        }
    }

    private void drawTicks(Canvas canvas, float cx, float cy, float radius) {
        float tickLen = 4f * density;
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float innerR = radius - tickLen;
            canvas.drawLine(
                    cx + cos * radius, cy + sin * radius,
                    cx + cos * innerR, cy + sin * innerR,
                    tickPaint);
        }
    }

    private void drawAxisLabels(Canvas canvas, float cx, float cy, float radius) {
        float labelOffset = 8f * density;
        canvas.drawText("左", cx - radius - labelOffset, cy + labelTextPaint.getTextSize() / 3f, labelTextPaint);
        canvas.drawText("右", cx + radius + labelOffset, cy + labelTextPaint.getTextSize() / 3f, labelTextPaint);
        canvas.drawText("前", cx, cy - radius - labelOffset, labelTextPaint);
        canvas.drawText("后", cx, cy + radius + labelTextPaint.getTextSize() + labelOffset, labelTextPaint);
    }

    private void drawBall(Canvas canvas, float ballX, float ballY) {
        canvas.drawCircle(ballX, ballY, ballRadiusPx, ballFillPaint);
        canvas.drawCircle(ballX, ballY, ballRadiusPx, ballStrokePaint);
    }

    private void drawCenterValue(Canvas canvas, float cx, float cy) {
        String gText = String.format("%.2fG", filteredMagnitude);
        canvas.drawText(gText, cx, cy + valueTextPaint.getTextSize() / 3f, valueTextPaint);
        canvas.drawText("合成 G 值", cx, cy - 18f * density, labelTextPaint);
    }

    private float centerX() {
        return getWidth() / 2f;
    }

    private float centerY() {
        return getHeight() / 2f;
    }

    private float usableRadius() {
        float w = getWidth();
        float h = getHeight();
        float minDim = Math.min(w, h);
        return (minDim / 2f) - (BALL_RADIUS_DP + 6f) * density;
    }

    private float clampBallX(float x) {
        float cx = centerX();
        float r = usableRadius();
        return Math.max(cx - r, Math.min(cx + r, x));
    }

    private float clampBallY(float y) {
        float cy = centerY();
        float r = usableRadius();
        return Math.max(cy - r, Math.min(cy + r, y));
    }
}
