package com.acooldog.toolbox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

final class AlgorithmChartView extends View {
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<Float> primaryValues = new ArrayList<>();
    private List<Float> secondaryValues = new ArrayList<>();
    private String label = "";

    AlgorithmChartView(Context context) {
        super(context);
        axisPaint.setColor(Color.parseColor("#B0BEC5"));
        axisPaint.setStrokeWidth(2f);
        linePaint.setStrokeWidth(3f);
        linePaint.setStyle(Paint.Style.STROKE);
        textPaint.setColor(Color.parseColor("#455A64"));
        textPaint.setTextSize(28f);
        setMinimumHeight(240);
    }

    void setSeries(@NonNull String label, @NonNull List<Float> primaryValues, @NonNull List<Float> secondaryValues) {
        this.label = label;
        this.primaryValues = new ArrayList<>(primaryValues);
        this.secondaryValues = new ArrayList<>(secondaryValues);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int left = 36;
        int right = width - 18;
        int top = 28;
        int bottom = height - 32;
        canvas.drawColor(Color.parseColor("#F7F9FC"));
        canvas.drawLine(left, bottom, right, bottom, axisPaint);
        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawText(label, left, 24, textPaint);
        drawSeries(canvas, primaryValues, Color.parseColor("#126B5E"), left, top, right, bottom);
        drawSeries(canvas, secondaryValues, Color.parseColor("#C96A3D"), left, top, right, bottom);
    }

    private void drawSeries(
            Canvas canvas,
            List<Float> values,
            int color,
            int left,
            int top,
            int right,
            int bottom
    ) {
        if (values.size() < 2) {
            return;
        }
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (float value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        if (Math.abs(max - min) < 0.0001f) {
            max = min + 1f;
        }
        linePaint.setColor(color);
        float previousX = left;
        float previousY = scaleY(values.get(0), min, max, top, bottom);
        for (int index = 1; index < values.size(); index++) {
            float x = left + ((right - left) * (index / (float) (values.size() - 1)));
            float y = scaleY(values.get(index), min, max, top, bottom);
            canvas.drawLine(previousX, previousY, x, y, linePaint);
            previousX = x;
            previousY = y;
        }
    }

    private float scaleY(float value, float min, float max, int top, int bottom) {
        float ratio = (value - min) / (max - min);
        return bottom - ((bottom - top) * ratio);
    }
}
