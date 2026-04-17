package com.zcshou.gogogo.route.domain.service;

import com.zcshou.gogogo.route.domain.model.RouteDefinition;
import com.zcshou.gogogo.route.domain.model.RoutePoint;
import com.zcshou.gogogo.route.domain.model.RouteSimulationConfig;
import com.zcshou.gogogo.route.domain.model.SimulationFrame;

import java.util.List;

public final class RouteSimulationEngine {
    private final RouteDefinition routeDefinition;
    private final RouteSimulationConfig config;
    private int currentSegmentIndex;
    private double progressInSegmentMeters;
    private int completedLoops;
    private boolean finished;

    public RouteSimulationEngine(RouteDefinition routeDefinition, RouteSimulationConfig config) {
        if (!routeDefinition.hasEnoughPoints()) {
            throw new IllegalArgumentException("Route must contain at least two points");
        }
        this.routeDefinition = routeDefinition;
        this.config = config;
    }

    public SimulationFrame next(double speedMultiplier) {
        List<RoutePoint> points = routeDefinition.getPoints();
        RoutePoint currentPoint = points.get(currentSegmentIndex);
        RoutePoint nextPoint = points.get(currentSegmentIndex + 1);
        double distanceStep = config.getSpeedMetersPerSecond() * speedMultiplier * (config.getTickMillis() / 1000.0d);

        while (distanceStep >= 0 && !finished) {
            double segmentDistance = distanceMeters(currentPoint, nextPoint);
            if (segmentDistance <= 0.01d) {
                advanceSegment(points);
                if (finished) {
                    break;
                }
                currentPoint = points.get(currentSegmentIndex);
                nextPoint = points.get(currentSegmentIndex + 1);
                continue;
            }

            double remainingInSegment = segmentDistance - progressInSegmentMeters;
            if (distanceStep < remainingInSegment) {
                progressInSegmentMeters += distanceStep;
                double ratio = progressInSegmentMeters / segmentDistance;
                RoutePoint interpolatedPoint = interpolate(currentPoint, nextPoint, ratio);
                return new SimulationFrame(
                        interpolatedPoint,
                        bearingDegrees(currentPoint, nextPoint),
                        (float) (config.getSpeedMetersPerSecond() * speedMultiplier),
                        false,
                        completedLoops
                );
            }

            distanceStep -= remainingInSegment;
            advanceSegment(points);
            if (!finished) {
                currentPoint = points.get(currentSegmentIndex);
                nextPoint = points.get(currentSegmentIndex + 1);
            }
        }

        RoutePoint lastPoint = points.get(points.size() - 1);
        return new SimulationFrame(lastPoint, 0f, 0f, true, completedLoops);
    }

    public boolean isFinished() {
        return finished;
    }

    private void advanceSegment(List<RoutePoint> points) {
        progressInSegmentMeters = 0d;
        if (currentSegmentIndex < points.size() - 2) {
            currentSegmentIndex++;
            return;
        }

        completedLoops++;
        if (completedLoops >= config.getLoopCount()) {
            finished = true;
            return;
        }
        currentSegmentIndex = 0;
    }

    private RoutePoint interpolate(RoutePoint start, RoutePoint end, double ratio) {
        double bdLongitude = lerp(start.getBdLongitude(), end.getBdLongitude(), ratio);
        double bdLatitude = lerp(start.getBdLatitude(), end.getBdLatitude(), ratio);
        double wgsLongitude = lerp(start.getWgsLongitude(), end.getWgsLongitude(), ratio);
        double wgsLatitude = lerp(start.getWgsLatitude(), end.getWgsLatitude(), ratio);
        double altitude = lerp(start.getAltitude(), end.getAltitude(), ratio);
        return new RoutePoint(bdLongitude, bdLatitude, wgsLongitude, wgsLatitude, altitude);
    }

    private double lerp(double start, double end, double ratio) {
        return start + ((end - start) * ratio);
    }

    private double distanceMeters(RoutePoint start, RoutePoint end) {
        double earthRadius = 6371000d;
        double lat1 = Math.toRadians(start.getWgsLatitude());
        double lat2 = Math.toRadians(end.getWgsLatitude());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(end.getWgsLongitude() - start.getWgsLongitude());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private float bearingDegrees(RoutePoint start, RoutePoint end) {
        double startLat = Math.toRadians(start.getWgsLatitude());
        double endLat = Math.toRadians(end.getWgsLatitude());
        double deltaLon = Math.toRadians(end.getWgsLongitude() - start.getWgsLongitude());
        double y = Math.sin(deltaLon) * Math.cos(endLat);
        double x = Math.cos(startLat) * Math.sin(endLat)
                - Math.sin(startLat) * Math.cos(endLat) * Math.cos(deltaLon);
        double bearing = Math.toDegrees(Math.atan2(y, x));
        if (bearing < 0) {
            bearing += 360d;
        }
        return (float) bearing;
    }
}
