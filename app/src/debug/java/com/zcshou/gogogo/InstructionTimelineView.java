package com.acooldog.toolbox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestInstruction;

import java.util.ArrayList;
import java.util.List;

final class InstructionTimelineView extends View {
    interface SelectionListener {
        void onInstructionSelected(int index);
    }

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<TestInstruction> instructions = new ArrayList<>();
    private int selectedIndex = -1;
    private SelectionListener selectionListener;

    InstructionTimelineView(Context context) {
        super(context);
        linePaint.setColor(Color.parseColor("#B0BEC5"));
        linePaint.setStrokeWidth(4f);
        nodePaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.parseColor("#263238"));
        textPaint.setTextSize(24f);
        setMinimumHeight(180);
    }

    void setSelectionListener(SelectionListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    void setInstructions(@NonNull List<TestInstruction> instructions, int selectedIndex) {
        this.instructions = new ArrayList<>(instructions);
        this.selectedIndex = selectedIndex;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.parseColor("#F7F9FC"));
        int width = getWidth();
        int height = getHeight();
        int centerY = Math.max(60, height / 2);
        int left = 36;
        int right = width - 36;
        canvas.drawLine(left, centerY, right, centerY, linePaint);
        if (instructions.isEmpty()) {
            canvas.drawText("暂无指令", left, centerY - 22, textPaint);
            return;
        }
        for (int index = 0; index < instructions.size(); index++) {
            float x = xForIndex(index, left, right);
            nodePaint.setColor(index == selectedIndex ? Color.parseColor("#C96A3D") : Color.parseColor("#126B5E"));
            canvas.drawCircle(x, centerY, index == selectedIndex ? 18f : 14f, nodePaint);
            String label = (index + 1) + "." + instructions.get(index).getAction().name();
            canvas.drawText(label, Math.max(8f, x - 70f), centerY + 48f + ((index % 2) * 28f), textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP || instructions.isEmpty()) {
            return true;
        }
        int left = 36;
        int right = getWidth() - 36;
        int nearest = 0;
        float nearestDistance = Float.MAX_VALUE;
        for (int index = 0; index < instructions.size(); index++) {
            float distance = Math.abs(event.getX() - xForIndex(index, left, right));
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = index;
            }
        }
        selectedIndex = nearest;
        if (selectionListener != null) {
            selectionListener.onInstructionSelected(nearest);
        }
        invalidate();
        return true;
    }

    private float xForIndex(int index, int left, int right) {
        if (instructions.size() == 1) {
            return (left + right) / 2f;
        }
        return left + ((right - left) * (index / (float) (instructions.size() - 1)));
    }
}
