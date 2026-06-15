package com.acooldog.toolbox.route.domain.service;

import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.domain.model.RoutePoint;
import com.acooldog.toolbox.route.domain.model.RouteSimulationConfig;
import com.acooldog.toolbox.route.domain.model.SimulationFrame;

import java.util.List;
import java.util.Random;

public final class RouteSimulationEngine {
    private static final double EARTH_RADIUS_METERS = 6371000d;
    private static final double ESTIMATED_STEP_LENGTH_METERS = 0.8d;
    private static final double MINIMUM_SIMULATION_SPEED_METERS_PER_SECOND = 0.5d;
    private static final double MINIMUM_DYNAMIC_SPEED_RATIO = 0.35d;
    private static final double INTENSITY_SETTLE_EPSILON_METERS_PER_SECOND = 0.05d;
    private static final double PATH_SETTLE_EPSILON_METERS = 0.05d;
    private static final double ALTITUDE_SETTLE_EPSILON_METERS = 0.03d;
    private static final double MINIMUM_ROUTE_SEGMENT_DISTANCE_METERS = 0.01d;

    private RouteDefinition routeDefinition;
    private RouteSimulationConfig config;
    private double routeDistanceMeters;
    private final Random random;
    private int currentSegmentIndex;
    private double progressInSegmentMeters;
    private int completedLoops;
    private boolean finished;
    private double currentIntensityOffsetMetersPerSecond;
    private double targetIntensityOffsetMetersPerSecond;
    private double currentPathOffsetMeters;
    private double targetPathOffsetMeters;
    private double currentAltitudeOffsetMeters;
    private double targetAltitudeOffsetMeters;

    public RouteSimulationEngine(RouteDefinition routeDefinition, RouteSimulationConfig config) {
        this(routeDefinition, config, new Random());
    }

    RouteSimulationEngine(RouteDefinition routeDefinition, RouteSimulationConfig config, Random random) {
        if (!routeDefinition.hasEnoughPoints()) {
            throw new IllegalArgumentException("Route must contain at least two points");
        }
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.routeDefinition = routeDefinition;
        this.config = config;
        this.routeDistanceMeters = calculateRouteDistanceMeters(routeDefinition.getPoints());
        this.random = random;
    }

    public SimulationFrame next() {
        List<RoutePoint> points = routeDefinition.getPoints();
        RoutePoint currentPoint = points.get(currentSegmentIndex);
        RoutePoint nextPoint = points.get(currentSegmentIndex + 1);
        double resolvedSpeed = resolveCurrentSpeedMetersPerSecond();
        double distanceStep = resolvedSpeed * (config.getTickMillis() / 1000.0d);

        while (distanceStep >= 0 && !finished) {
            double segmentDistance = distanceMeters(currentPoint, nextPoint);
            if (segmentDistance <= MINIMUM_ROUTE_SEGMENT_DISTANCE_METERS) {
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
                float bearing = bearingDegrees(currentPoint, nextPoint);
                RoutePoint simulatedPoint = applyNaturalAltitudeVariation(
                        applyNaturalPathVariation(interpolatedPoint, bearing)
                );
                return new SimulationFrame(
                        simulatedPoint,
                        bearing,
                        (float) resolvedSpeed,
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

        RoutePoint lastPoint = applyNaturalAltitudeVariation(points.get(points.size() - 1));
        return new SimulationFrame(lastPoint, 0f, 0f, true, completedLoops);
    }

    public boolean isFinished() {
        return finished;
    }

    public void updateConfig(RouteSimulationConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
        if (!config.isDynamicIntensityEnabled()) {
            currentIntensityOffsetMetersPerSecond = 0d;
            targetIntensityOffsetMetersPerSecond = 0d;
        }
        if (!config.isNaturalPathVariationEnabled()) {
            currentPathOffsetMeters = 0d;
            targetPathOffsetMeters = 0d;
        }
        if (!config.isNaturalAltitudeVariationEnabled()) {
            currentAltitudeOffsetMeters = 0d;
            targetAltitudeOffsetMeters = 0d;
        }
        if (completedLoops >= config.getLoopCount()) {
            finished = true;
        }
    }

    public void updateRoute(RouteDefinition routeDefinition) {
        if (routeDefinition == null || !routeDefinition.hasEnoughPoints()) {
            throw new IllegalArgumentException("routeDefinition must contain at least two points");
        }
        double distanceTraveledInCurrentLoop = resolveDistanceTraveledInCurrentLoop(this.routeDefinition.getPoints());
        this.routeDefinition = routeDefinition;
        this.routeDistanceMeters = calculateRouteDistanceMeters(routeDefinition.getPoints());
        realignProgressToUpdatedRoute(distanceTraveledInCurrentLoop);
    }

    private double resolveCurrentSpeedMetersPerSecond() {
        double baseSpeed = config.getMode() == RouteSimulationConfig.Mode.CADENCE
                ? resolveCadenceSpeedMetersPerSecond()
                : config.getSpeedMetersPerSecond();
        if (baseSpeed <= 0d) {
            return 0d;
        }
        if (!config.isDynamicIntensityEnabled()
                || config.getIntensityVariationRangeMetersPerSecond() <= 0d
                || config.getIntensityVariationFrequency() <= 0d) {
            currentIntensityOffsetMetersPerSecond = 0d;
            targetIntensityOffsetMetersPerSecond = 0d;
            return baseSpeed;
        }

        double range = config.getIntensityVariationRangeMetersPerSecond();
        double frequency = config.getIntensityVariationFrequency();
        if (Math.abs(targetIntensityOffsetMetersPerSecond - currentIntensityOffsetMetersPerSecond)
                <= INTENSITY_SETTLE_EPSILON_METERS_PER_SECOND
                || random.nextDouble() < frequency) {
            targetIntensityOffsetMetersPerSecond = randomInRange(range);
        }
        double smoothing = 0.15d + (frequency * 0.55d);
        currentIntensityOffsetMetersPerSecond +=
                (targetIntensityOffsetMetersPerSecond - currentIntensityOffsetMetersPerSecond) * smoothing;
        currentIntensityOffsetMetersPerSecond = clamp(
                currentIntensityOffsetMetersPerSecond,
                -range,
                range
        );

        double minSpeed = Math.max(MINIMUM_SIMULATION_SPEED_METERS_PER_SECOND, baseSpeed * MINIMUM_DYNAMIC_SPEED_RATIO);
        return clamp(baseSpeed + currentIntensityOffsetMetersPerSecond, minSpeed, baseSpeed + range);
    }

    private double resolveCadenceSpeedMetersPerSecond() {
        double totalDistanceMeters = routeDistanceMeters * config.getLoopCount();
        if (totalDistanceMeters <= 0d) {
            return 0d;
        }

        // Average cadence = total steps / total duration(minutes).
        // Route simulation has no stride input, so total steps are estimated from distance and a fixed step length.
        double totalSteps = totalDistanceMeters / ESTIMATED_STEP_LENGTH_METERS;
        double totalDurationMinutes = totalSteps / config.getCadenceStepsPerMinute();
        if (totalDurationMinutes <= 0d) {
            return 0d;
        }
        return totalDistanceMeters / (totalDurationMinutes * 60.0d);
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

    private double resolveDistanceTraveledInCurrentLoop(List<RoutePoint> points) {
        double distance = 0d;
        if (points == null || points.size() < 2) {
            return distance;
        }
        int maxSegmentIndex = Math.min(currentSegmentIndex, points.size() - 2);
        for (int index = 0; index < maxSegmentIndex; index++) {
            distance += distanceMeters(points.get(index), points.get(index + 1));
        }
        return distance + Math.max(0d, progressInSegmentMeters);
    }

    private void realignProgressToUpdatedRoute(double distanceTraveledInCurrentLoop) {
        List<RoutePoint> points = routeDefinition.getPoints();
        if (points.size() < 2) {
            finished = true;
            return;
        }
        currentSegmentIndex = 0;
        progressInSegmentMeters = 0d;
        double remainingDistance = Math.max(0d, distanceTraveledInCurrentLoop);
        for (int index = 0; index < points.size() - 1; index++) {
            double segmentDistance = distanceMeters(points.get(index), points.get(index + 1));
            if (index == points.size() - 2) {
                currentSegmentIndex = index;
                progressInSegmentMeters = Math.min(remainingDistance, segmentDistance);
                return;
            }
            if (remainingDistance <= segmentDistance) {
                currentSegmentIndex = index;
                progressInSegmentMeters = remainingDistance;
                return;
            }
            remainingDistance -= segmentDistance;
        }
    }

    private RoutePoint applyNaturalPathVariation(RoutePoint basePoint, float bearingDegrees) {
        if (!config.isNaturalPathVariationEnabled() || config.getPathVariationAmplitudeMeters() <= 0d) {
            currentPathOffsetMeters = 0d;
            targetPathOffsetMeters = 0d;
            return basePoint;
        }

        double amplitude = config.getPathVariationAmplitudeMeters();
        if (Math.abs(targetPathOffsetMeters - currentPathOffsetMeters) <= PATH_SETTLE_EPSILON_METERS
                || random.nextDouble() < 0.25d) {
            targetPathOffsetMeters = randomInRange(amplitude);
        }
        currentPathOffsetMeters += (targetPathOffsetMeters - currentPathOffsetMeters) * 0.18d;
        currentPathOffsetMeters = clamp(currentPathOffsetMeters, -amplitude, amplitude);

        if (Math.abs(currentPathOffsetMeters) <= PATH_SETTLE_EPSILON_METERS) {
            return basePoint;
        }

        CoordinateOffset bdOffset = offsetCoordinate(
                basePoint.getBdLatitude(),
                basePoint.getBdLongitude(),
                currentPathOffsetMeters,
                bearingDegrees + 90d
        );
        CoordinateOffset wgsOffset = offsetCoordinate(
                basePoint.getWgsLatitude(),
                basePoint.getWgsLongitude(),
                currentPathOffsetMeters,
                bearingDegrees + 90d
        );
        return new RoutePoint(
                bdOffset.longitude,
                bdOffset.latitude,
                wgsOffset.longitude,
                wgsOffset.latitude,
                basePoint.getAltitude()
        );
    }

    private RoutePoint applyNaturalAltitudeVariation(RoutePoint basePoint) {
        if (!config.isNaturalAltitudeVariationEnabled()) {
            currentAltitudeOffsetMeters = 0d;
            targetAltitudeOffsetMeters = 0d;
            return basePoint;
        }

        double baseAltitude = config.getAltitudeBaseMeters() + (config.getAltitudeVariationHeightCentimeters() / 100d);
        double range = config.getAltitudeVariationRangeMeters();
        double probability = config.getAltitudeVariationProbability();
        if (range <= 0d || probability <= 0d) {
            currentAltitudeOffsetMeters = 0d;
            targetAltitudeOffsetMeters = 0d;
            return new RoutePoint(
                    basePoint.getBdLongitude(),
                    basePoint.getBdLatitude(),
                    basePoint.getWgsLongitude(),
                    basePoint.getWgsLatitude(),
                    baseAltitude
            );
        }

        if (Math.abs(targetAltitudeOffsetMeters - currentAltitudeOffsetMeters) <= ALTITUDE_SETTLE_EPSILON_METERS
                || random.nextDouble() < probability) {
            targetAltitudeOffsetMeters = randomInRange(range);
        }
        double smoothing = 0.12d + (probability * 0.55d);
        currentAltitudeOffsetMeters +=
                (targetAltitudeOffsetMeters - currentAltitudeOffsetMeters) * smoothing;
        currentAltitudeOffsetMeters = clamp(
                currentAltitudeOffsetMeters,
                -range,
                range
        );
        return new RoutePoint(
                basePoint.getBdLongitude(),
                basePoint.getBdLatitude(),
                basePoint.getWgsLongitude(),
                basePoint.getWgsLatitude(),
                baseAltitude + currentAltitudeOffsetMeters
        );
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

    private double calculateRouteDistanceMeters(List<RoutePoint> points) {
        double totalDistance = 0d;
        for (int index = 0; index < points.size() - 1; index++) {
            totalDistance += distanceMeters(points.get(index), points.get(index + 1));
        }
        return totalDistance;
    }

    private double distanceMeters(RoutePoint start, RoutePoint end) {
        double lat1 = Math.toRadians(start.getWgsLatitude());
        double lat2 = Math.toRadians(end.getWgsLatitude());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(end.getWgsLongitude() - start.getWgsLongitude());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
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

    private CoordinateOffset offsetCoordinate(double latitude, double longitude, double offsetMeters, double bearingDegrees) {
        double headingRadians = Math.toRadians(bearingDegrees);
        double deltaNorth = offsetMeters * Math.cos(headingRadians);
        double deltaEast = offsetMeters * Math.sin(headingRadians);
        double latitudeRadians = Math.toRadians(latitude);
        double deltaLatitude = Math.toDegrees(deltaNorth / EARTH_RADIUS_METERS);
        double eastWestRadius = EARTH_RADIUS_METERS * Math.cos(latitudeRadians);
        double deltaLongitude = Math.abs(eastWestRadius) < 0.000001d
                ? 0d
                : Math.toDegrees(deltaEast / eastWestRadius);
        return new CoordinateOffset(latitude + deltaLatitude, longitude + deltaLongitude);
    }

    private double randomInRange(double maxAbsoluteValue) {
        return (random.nextDouble() * 2d - 1d) * maxAbsoluteValue;
    }

    private double clamp(double value, double minValue, double maxValue) {
        return Math.max(minValue, Math.min(maxValue, value));
    }

    private static final class CoordinateOffset {
        private final double latitude;
        private final double longitude;

        private CoordinateOffset(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
