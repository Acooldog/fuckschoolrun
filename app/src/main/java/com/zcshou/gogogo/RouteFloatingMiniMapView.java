package com.acooldog.toolbox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.domain.model.RoutePoint;

import java.util.ArrayList;
import java.util.List;

public final class RouteFloatingMiniMapView extends View {
    private static final float DRAW_PADDING_DP = 16f;

    private final Paint routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint startPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<RoutePoint> routePoints = new ArrayList<>();
    private double currentLongitude;
    private double currentLatitude;
    private boolean hasCurrentPoint;

    public RouteFloatingMiniMapView(Context context) {
        this(context, null);
    }

    public RouteFloatingMiniMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        routePaint.setColor(Color.parseColor("#1565C0"));
        routePaint.setStyle(Paint.Style.STROKE);
        routePaint.setStrokeCap(Paint.Cap.ROUND);
        routePaint.setStrokeJoin(Paint.Join.ROUND);
        routePaint.setStrokeWidth(dp(3f));

        markerPaint.setColor(Color.parseColor("#D32F2F"));
        markerPaint.setStyle(Paint.Style.FILL);

        startPaint.setColor(Color.parseColor("#2E7D32"));
        startPaint.setStyle(Paint.Style.FILL);
        setBackgroundColor(Color.parseColor("#F7F9FC"));
    }

    public void setRoute(@Nullable RouteDefinition routeDefinition) {
        routePoints.clear();
        if (routeDefinition != null && routeDefinition.getPoints() != null) {
            routePoints.addAll(routeDefinition.getPoints());
        }
        invalidate();
    }

    public void setCurrentPosition(double longitude, double latitude) {
        currentLongitude = longitude;
        currentLatitude = latitude;
        hasCurrentPoint = true;
        invalidate();
    }

    public void clearCurrentPosition() {
        hasCurrentPoint = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (routePoints.size() < 2) {
            return;
        }

        float padding = dp(DRAW_PADDING_DP);
        float width = Math.max(1f, getWidth() - (padding * 2f));
        float height = Math.max(1f, getHeight() - (padding * 2f));

        Bounds bounds = Bounds.from(routePoints, hasCurrentPoint ? currentLongitude : Double.NaN, hasCurrentPoint ? currentLatitude : Double.NaN);
        if (bounds == null) {
            return;
        }

        Path routePath = new Path();
        boolean first = true;
        for (RoutePoint point : routePoints) {
            float x = projectX(point.getWgsLongitude(), bounds, padding, width);
            float y = projectY(point.getWgsLatitude(), bounds, padding, height);
            if (first) {
                routePath.moveTo(x, y);
                first = false;
            } else {
                routePath.lineTo(x, y);
            }
        }
        canvas.drawPath(routePath, routePaint);

        RoutePoint startPoint = routePoints.get(0);
        canvas.drawCircle(
                projectX(startPoint.getWgsLongitude(), bounds, padding, width),
                projectY(startPoint.getWgsLatitude(), bounds, padding, height),
                dp(5f),
                startPaint
        );

        if (hasCurrentPoint) {
            canvas.drawCircle(
                    projectX(currentLongitude, bounds, padding, width),
                    projectY(currentLatitude, bounds, padding, height),
                    dp(6f),
                    markerPaint
            );
        }
    }

    private float projectX(double longitude, Bounds bounds, float padding, float width) {
        return padding + (float) ((longitude - bounds.minLongitude) / bounds.longitudeSpan) * width;
    }

    private float projectY(double latitude, Bounds bounds, float padding, float height) {
        return padding + (1f - (float) ((latitude - bounds.minLatitude) / bounds.latitudeSpan)) * height;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static final class Bounds {
        private final double minLongitude;
        private final double maxLongitude;
        private final double minLatitude;
        private final double maxLatitude;
        private final double longitudeSpan;
        private final double latitudeSpan;

        private Bounds(double minLongitude, double maxLongitude, double minLatitude, double maxLatitude) {
            this.minLongitude = minLongitude;
            this.maxLongitude = maxLongitude;
            this.minLatitude = minLatitude;
            this.maxLatitude = maxLatitude;
            this.longitudeSpan = Math.max(0.00001d, maxLongitude - minLongitude);
            this.latitudeSpan = Math.max(0.00001d, maxLatitude - minLatitude);
        }

        @Nullable
        private static Bounds from(List<RoutePoint> points, double currentLongitude, double currentLatitude) {
            if (points == null || points.isEmpty()) {
                return null;
            }
            double minLongitude = Double.MAX_VALUE;
            double maxLongitude = -Double.MAX_VALUE;
            double minLatitude = Double.MAX_VALUE;
            double maxLatitude = -Double.MAX_VALUE;
            for (RoutePoint point : points) {
                minLongitude = Math.min(minLongitude, point.getWgsLongitude());
                maxLongitude = Math.max(maxLongitude, point.getWgsLongitude());
                minLatitude = Math.min(minLatitude, point.getWgsLatitude());
                maxLatitude = Math.max(maxLatitude, point.getWgsLatitude());
            }
            if (!Double.isNaN(currentLongitude) && !Double.isNaN(currentLatitude)) {
                minLongitude = Math.min(minLongitude, currentLongitude);
                maxLongitude = Math.max(maxLongitude, currentLongitude);
                minLatitude = Math.min(minLatitude, currentLatitude);
                maxLatitude = Math.max(maxLatitude, currentLatitude);
            }
            double longitudeInset = Math.max((maxLongitude - minLongitude) * 0.08d, 0.00002d);
            double latitudeInset = Math.max((maxLatitude - minLatitude) * 0.08d, 0.00002d);
            return new Bounds(
                    minLongitude - longitudeInset,
                    maxLongitude + longitudeInset,
                    minLatitude - latitudeInset,
                    maxLatitude + latitudeInset
            );
        }
    }
}
