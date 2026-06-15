package com.acooldog.toolbox.route.domain.service;

import androidx.annotation.NonNull;

import com.acooldog.toolbox.route.domain.model.RoutePoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RouteEditUtils {
    private static final double EARTH_RADIUS_METERS = 6371000d;
    private static final double KEY_POINT_MAX_STRAIGHT_ANGLE_DEGREES = 170d;

    private RouteEditUtils() {
    }

    @NonNull
    public static List<RoutePoint> insertEvenlySpacedPoints(
            @NonNull List<RoutePoint> routePoints,
            int additionalPointCount
    ) {
        List<RoutePoint> sourcePoints = routePoints == null ? new ArrayList<>() : new ArrayList<>(routePoints);
        if (sourcePoints.size() < 2 || additionalPointCount <= 0) {
            return sourcePoints;
        }

        List<Double> cumulativeDistances = buildCumulativeDistances(sourcePoints);
        double totalDistance = cumulativeDistances.get(cumulativeDistances.size() - 1);
        if (totalDistance <= 0d) {
            return sourcePoints;
        }

        List<PointPlacement> placements = new ArrayList<>();
        for (int index = 0; index < sourcePoints.size(); index++) {
            placements.add(new PointPlacement(cumulativeDistances.get(index), sourcePoints.get(index), index, true));
        }

        for (int insertIndex = 1; insertIndex <= additionalPointCount; insertIndex++) {
            double targetDistance = totalDistance * (insertIndex / (double) (additionalPointCount + 1));
            int segmentIndex = findSegmentIndexForDistance(cumulativeDistances, targetDistance);
            double segmentStartDistance = cumulativeDistances.get(segmentIndex);
            double segmentEndDistance = cumulativeDistances.get(segmentIndex + 1);
            double segmentDistance = Math.max(0.000001d, segmentEndDistance - segmentStartDistance);
            double ratio = (targetDistance - segmentStartDistance) / segmentDistance;
            placements.add(new PointPlacement(
                    targetDistance,
                    interpolate(sourcePoints.get(segmentIndex), sourcePoints.get(segmentIndex + 1), ratio),
                    insertIndex,
                    false
            ));
        }

        placements.sort(Comparator
                .comparingDouble((PointPlacement placement) -> placement.distanceFromStart)
                .thenComparing((PointPlacement placement) -> !placement.original)
                .thenComparingInt(placement -> placement.sequence));

        List<RoutePoint> result = new ArrayList<>(placements.size());
        for (PointPlacement placement : placements) {
            result.add(placement.routePoint);
        }
        return result;
    }

    public static int countKeyRoutePoints(@NonNull List<RoutePoint> routePoints) {
        List<RoutePoint> sourcePoints = routePoints == null ? new ArrayList<>() : routePoints;
        int keyPointCount = 0;
        for (int index = 0; index < sourcePoints.size(); index++) {
            if (isKeyRoutePoint(sourcePoints, index)) {
                keyPointCount++;
            }
        }
        return keyPointCount;
    }

    public static int countRemovableRoutePoints(@NonNull List<RoutePoint> routePoints) {
        List<RoutePoint> sourcePoints = routePoints == null ? new ArrayList<>() : routePoints;
        return Math.max(0, sourcePoints.size() - countKeyRoutePoints(sourcePoints));
    }

    @NonNull
    public static List<RoutePoint> removeEvenlySpacedNonKeyPoints(
            @NonNull List<RoutePoint> routePoints,
            int removePointCount
    ) {
        List<RoutePoint> sourcePoints = routePoints == null ? new ArrayList<>() : new ArrayList<>(routePoints);
        if (sourcePoints.size() < 3 || removePointCount <= 0) {
            return sourcePoints;
        }

        List<Integer> removableIndices = new ArrayList<>();
        for (int index = 0; index < sourcePoints.size(); index++) {
            if (!isKeyRoutePoint(sourcePoints, index)) {
                removableIndices.add(index);
            }
        }
        if (removableIndices.isEmpty()) {
            return sourcePoints;
        }

        boolean[] shouldRemove = new boolean[sourcePoints.size()];
        int clampedRemoveCount = Math.min(removePointCount, removableIndices.size());
        if (clampedRemoveCount == removableIndices.size()) {
            for (int index : removableIndices) {
                shouldRemove[index] = true;
            }
        } else {
            int previousPosition = -1;
            for (int removalIndex = 1; removalIndex <= clampedRemoveCount; removalIndex++) {
                int removablePosition = (int) Math.floor(
                        removalIndex * ((removableIndices.size() + 1d) / (clampedRemoveCount + 1d))
                ) - 1;
                removablePosition = Math.max(previousPosition + 1,
                        Math.min(removableIndices.size() - 1, removablePosition));
                shouldRemove[removableIndices.get(removablePosition)] = true;
                previousPosition = removablePosition;
            }
        }

        List<RoutePoint> result = new ArrayList<>(sourcePoints.size() - clampedRemoveCount);
        for (int index = 0; index < sourcePoints.size(); index++) {
            if (!shouldRemove[index]) {
                result.add(sourcePoints.get(index));
            }
        }
        return result;
    }

    @NonNull
    public static List<RoutePoint> smoothRightAngles(
            @NonNull List<RoutePoint> routePoints,
            double radiusMeters,
            double minAngleDegrees,
            double maxAngleDegrees
    ) {
        List<RoutePoint> sourcePoints = routePoints == null ? new ArrayList<>() : new ArrayList<>(routePoints);
        if (sourcePoints.size() < 3 || radiusMeters <= 0d) {
            return sourcePoints;
        }

        List<RoutePoint> result = new ArrayList<>();
        result.add(sourcePoints.get(0));
        for (int index = 1; index < sourcePoints.size() - 1; index++) {
            RoutePoint previous = sourcePoints.get(index - 1);
            RoutePoint current = sourcePoints.get(index);
            RoutePoint next = sourcePoints.get(index + 1);
            double angleDegrees = estimateAngleDegrees(previous, current, next);
            double previousSegmentDistance = distanceMeters(previous, current);
            double nextSegmentDistance = distanceMeters(current, next);
            if (angleDegrees >= minAngleDegrees
                    && angleDegrees <= maxAngleDegrees
                    && previousSegmentDistance > 0.5d
                    && nextSegmentDistance > 0.5d) {
                double trimPrevious = Math.min(radiusMeters, previousSegmentDistance / 2.5d);
                double trimNext = Math.min(radiusMeters, nextSegmentDistance / 2.5d);
                RoutePoint entryPoint = interpolate(previous, current, (previousSegmentDistance - trimPrevious) / previousSegmentDistance);
                RoutePoint exitPoint = interpolate(current, next, trimNext / nextSegmentDistance);
                result.add(entryPoint);
                result.add(quadraticBezier(entryPoint, current, exitPoint, 0.5d));
                result.add(exitPoint);
            } else {
                result.add(current);
            }
        }
        result.add(sourcePoints.get(sourcePoints.size() - 1));
        return result;
    }

    @NonNull
    public static ProjectionResult projectPointOntoRoute(
            @NonNull List<RoutePoint> routePoints,
            @NonNull RoutePoint targetPoint
    ) {
        List<RoutePoint> sourcePoints = routePoints == null ? new ArrayList<>() : new ArrayList<>(routePoints);
        if (sourcePoints.size() < 2 || targetPoint == null) {
            return ProjectionResult.empty();
        }

        ProjectionResult bestProjection = ProjectionResult.empty();
        for (int index = 0; index < sourcePoints.size() - 1; index++) {
            RoutePoint start = sourcePoints.get(index);
            RoutePoint end = sourcePoints.get(index + 1);
            CoordinateDelta segmentVector = deltaMeters(start, end);
            CoordinateDelta targetVector = deltaMeters(start, targetPoint);
            double segmentLengthSquared = segmentVector.eastMeters * segmentVector.eastMeters
                    + segmentVector.northMeters * segmentVector.northMeters;
            if (segmentLengthSquared <= 0.000001d) {
                continue;
            }

            double rawRatio = (targetVector.eastMeters * segmentVector.eastMeters
                    + targetVector.northMeters * segmentVector.northMeters) / segmentLengthSquared;
            double clampedRatio = Math.max(0d, Math.min(1d, rawRatio));
            RoutePoint projectedPoint = interpolate(start, end, clampedRatio);
            double distanceMeters = distanceMeters(projectedPoint, targetPoint);
            if (!bestProjection.isValid() || distanceMeters < bestProjection.getDistanceToRouteMeters()) {
                bestProjection = new ProjectionResult(index, projectedPoint, clampedRatio, distanceMeters);
            }
        }
        return bestProjection;
    }

    @NonNull
    public static List<RoutePoint> insertPointOnRoute(
            @NonNull List<RoutePoint> routePoints,
            @NonNull ProjectionResult projectionResult
    ) {
        List<RoutePoint> sourcePoints = routePoints == null ? new ArrayList<>() : new ArrayList<>(routePoints);
        if (!projectionResult.isValid() || sourcePoints.size() < 2) {
            return sourcePoints;
        }
        int insertIndex = Math.max(0, Math.min(sourcePoints.size(), projectionResult.getSegmentStartIndex() + 1));
        sourcePoints.add(insertIndex, projectionResult.getProjectedPoint());
        return sourcePoints;
    }

    private static int findSegmentIndexForDistance(List<Double> cumulativeDistances, double targetDistance) {
        for (int index = 0; index < cumulativeDistances.size() - 1; index++) {
            double segmentEndDistance = cumulativeDistances.get(index + 1);
            if (targetDistance <= segmentEndDistance) {
                return index;
            }
        }
        return Math.max(0, cumulativeDistances.size() - 2);
    }

    @NonNull
    private static List<Double> buildCumulativeDistances(List<RoutePoint> routePoints) {
        List<Double> cumulativeDistances = new ArrayList<>(routePoints.size());
        double totalDistance = 0d;
        cumulativeDistances.add(0d);
        for (int index = 1; index < routePoints.size(); index++) {
            totalDistance += distanceMeters(routePoints.get(index - 1), routePoints.get(index));
            cumulativeDistances.add(totalDistance);
        }
        return cumulativeDistances;
    }

    private static boolean isKeyRoutePoint(List<RoutePoint> routePoints, int index) {
        if (routePoints == null || routePoints.isEmpty()) {
            return false;
        }
        if (index <= 0 || index >= routePoints.size() - 1) {
            return index >= 0 && index < routePoints.size();
        }
        double angleDegrees = estimateAngleDegrees(
                routePoints.get(index - 1),
                routePoints.get(index),
                routePoints.get(index + 1)
        );
        return angleDegrees < KEY_POINT_MAX_STRAIGHT_ANGLE_DEGREES;
    }

    private static double estimateAngleDegrees(RoutePoint previous, RoutePoint current, RoutePoint next) {
        CoordinateDelta incoming = deltaMeters(current, previous);
        CoordinateDelta outgoing = deltaMeters(current, next);
        double incomingLength = Math.hypot(incoming.eastMeters, incoming.northMeters);
        double outgoingLength = Math.hypot(outgoing.eastMeters, outgoing.northMeters);
        if (incomingLength <= 0.000001d || outgoingLength <= 0.000001d) {
            return 180d;
        }
        double dotProduct = incoming.eastMeters * outgoing.eastMeters
                + incoming.northMeters * outgoing.northMeters;
        double normalized = dotProduct / (incomingLength * outgoingLength);
        normalized = Math.max(-1d, Math.min(1d, normalized));
        return Math.toDegrees(Math.acos(normalized));
    }

    private static CoordinateDelta deltaMeters(RoutePoint origin, RoutePoint target) {
        double latitudeRadians = Math.toRadians(origin.getWgsLatitude());
        double northMeters = Math.toRadians(target.getWgsLatitude() - origin.getWgsLatitude()) * EARTH_RADIUS_METERS;
        double eastMeters = Math.toRadians(target.getWgsLongitude() - origin.getWgsLongitude())
                * EARTH_RADIUS_METERS * Math.cos(latitudeRadians);
        return new CoordinateDelta(eastMeters, northMeters);
    }

    private static double distanceMeters(RoutePoint start, RoutePoint end) {
        double lat1 = Math.toRadians(start.getWgsLatitude());
        double lat2 = Math.toRadians(end.getWgsLatitude());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(end.getWgsLongitude() - start.getWgsLongitude());
        double a = Math.sin(deltaLat / 2d) * Math.sin(deltaLat / 2d)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2d) * Math.sin(deltaLon / 2d);
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
        return EARTH_RADIUS_METERS * c;
    }

    private static RoutePoint quadraticBezier(RoutePoint start, RoutePoint control, RoutePoint end, double t) {
        double oneMinusT = 1d - t;
        double bdLongitude = oneMinusT * oneMinusT * start.getBdLongitude()
                + 2d * oneMinusT * t * control.getBdLongitude()
                + t * t * end.getBdLongitude();
        double bdLatitude = oneMinusT * oneMinusT * start.getBdLatitude()
                + 2d * oneMinusT * t * control.getBdLatitude()
                + t * t * end.getBdLatitude();
        double wgsLongitude = oneMinusT * oneMinusT * start.getWgsLongitude()
                + 2d * oneMinusT * t * control.getWgsLongitude()
                + t * t * end.getWgsLongitude();
        double wgsLatitude = oneMinusT * oneMinusT * start.getWgsLatitude()
                + 2d * oneMinusT * t * control.getWgsLatitude()
                + t * t * end.getWgsLatitude();
        double altitude = oneMinusT * oneMinusT * start.getAltitude()
                + 2d * oneMinusT * t * control.getAltitude()
                + t * t * end.getAltitude();
        return new RoutePoint(bdLongitude, bdLatitude, wgsLongitude, wgsLatitude, altitude);
    }

    private static RoutePoint interpolate(RoutePoint start, RoutePoint end, double ratio) {
        double clampedRatio = Math.max(0d, Math.min(1d, ratio));
        return new RoutePoint(
                lerp(start.getBdLongitude(), end.getBdLongitude(), clampedRatio),
                lerp(start.getBdLatitude(), end.getBdLatitude(), clampedRatio),
                lerp(start.getWgsLongitude(), end.getWgsLongitude(), clampedRatio),
                lerp(start.getWgsLatitude(), end.getWgsLatitude(), clampedRatio),
                lerp(start.getAltitude(), end.getAltitude(), clampedRatio)
        );
    }

    private static double lerp(double start, double end, double ratio) {
        return start + ((end - start) * ratio);
    }

    public static final class ProjectionResult {
        private final int segmentStartIndex;
        private final RoutePoint projectedPoint;
        private final double segmentRatio;
        private final double distanceToRouteMeters;

        private ProjectionResult(int segmentStartIndex, RoutePoint projectedPoint, double segmentRatio, double distanceToRouteMeters) {
            this.segmentStartIndex = segmentStartIndex;
            this.projectedPoint = projectedPoint;
            this.segmentRatio = segmentRatio;
            this.distanceToRouteMeters = distanceToRouteMeters;
        }

        @NonNull
        public static ProjectionResult empty() {
            return new ProjectionResult(-1, null, 0d, Double.MAX_VALUE);
        }

        public boolean isValid() {
            return segmentStartIndex >= 0 && projectedPoint != null;
        }

        public int getSegmentStartIndex() {
            return segmentStartIndex;
        }

        @NonNull
        public RoutePoint getProjectedPoint() {
            return projectedPoint;
        }

        public double getSegmentRatio() {
            return segmentRatio;
        }

        public double getDistanceToRouteMeters() {
            return distanceToRouteMeters;
        }
    }

    private static final class PointPlacement {
        private final double distanceFromStart;
        private final RoutePoint routePoint;
        private final int sequence;
        private final boolean original;

        private PointPlacement(double distanceFromStart, RoutePoint routePoint, int sequence, boolean original) {
            this.distanceFromStart = distanceFromStart;
            this.routePoint = routePoint;
            this.sequence = sequence;
            this.original = original;
        }
    }

    private static final class CoordinateDelta {
        private final double eastMeters;
        private final double northMeters;

        private CoordinateDelta(double eastMeters, double northMeters) {
            this.eastMeters = eastMeters;
            this.northMeters = northMeters;
        }
    }
}
