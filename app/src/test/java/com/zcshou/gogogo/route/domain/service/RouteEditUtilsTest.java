package com.acooldog.toolbox.route.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.acooldog.toolbox.route.domain.model.RoutePoint;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class RouteEditUtilsTest {
    @Test
    public void insertEvenlySpacedPoints_keepsOriginalPointsAndAddsUniformNodes() {
        List<RoutePoint> points = Arrays.asList(
                new RoutePoint(0.0d, 0.0d, 0.0d, 0.0d, 0.0d),
                new RoutePoint(0.001d, 0.0d, 0.001d, 0.0d, 0.0d)
        );

        List<RoutePoint> result = RouteEditUtils.insertEvenlySpacedPoints(points, 1);

        assertEquals(3, result.size());
        assertEquals(0.0d, result.get(0).getBdLongitude(), 0.0000001d);
        assertEquals(0.0005d, result.get(1).getBdLongitude(), 0.0000001d);
        assertEquals(0.001d, result.get(2).getBdLongitude(), 0.0000001d);
    }

    @Test
    public void smoothRightAngles_replacesNearRightAngleWithRoundedTransition() {
        List<RoutePoint> points = Arrays.asList(
                new RoutePoint(0.0d, 0.0d, 0.0d, 0.0d, 0.0d),
                new RoutePoint(0.001d, 0.0d, 0.001d, 0.0d, 0.0d),
                new RoutePoint(0.001d, 0.001d, 0.001d, 0.001d, 0.0d)
        );

        List<RoutePoint> result = RouteEditUtils.smoothRightAngles(points, 10d, 85d, 95d);

        assertTrue(result.size() > points.size());
        assertTrue(result.get(1).getBdLongitude() < 0.001d);
        assertTrue(result.get(2).getBdLatitude() > 0.0d);
        assertTrue(result.get(result.size() - 2).getBdLatitude() < 0.001d);
    }

    @Test
    public void insertPointOnRoute_projectsOntoNearestSegmentAndKeepsOrder() {
        List<RoutePoint> points = Arrays.asList(
                new RoutePoint(0.0d, 0.0d, 0.0d, 0.0d, 0.0d),
                new RoutePoint(0.001d, 0.0d, 0.001d, 0.0d, 0.0d),
                new RoutePoint(0.001d, 0.001d, 0.001d, 0.001d, 0.0d)
        );

        RouteEditUtils.ProjectionResult projectionResult = RouteEditUtils.projectPointOntoRoute(
                points,
                new RoutePoint(0.0006d, 0.0001d, 0.0006d, 0.0001d, 0.0d)
        );
        List<RoutePoint> result = RouteEditUtils.insertPointOnRoute(points, projectionResult);

        assertTrue(projectionResult.isValid());
        assertEquals(0, projectionResult.getSegmentStartIndex());
        assertEquals(4, result.size());
        assertEquals(0.0006d, result.get(1).getBdLongitude(), 0.0000001d);
        assertEquals(0.0d, result.get(1).getBdLatitude(), 0.0000001d);
    }

    @Test
    public void removeEvenlySpacedNonKeyPoints_keepsTurnsAndEndpoints() {
        List<RoutePoint> points = Arrays.asList(
                new RoutePoint(0.0d, 0.0d, 0.0d, 0.0d, 0.0d),
                new RoutePoint(0.0005d, 0.0d, 0.0005d, 0.0d, 0.0d),
                new RoutePoint(0.001d, 0.0d, 0.001d, 0.0d, 0.0d),
                new RoutePoint(0.001d, 0.0005d, 0.001d, 0.0005d, 0.0d),
                new RoutePoint(0.001d, 0.001d, 0.001d, 0.001d, 0.0d)
        );

        List<RoutePoint> result = RouteEditUtils.removeEvenlySpacedNonKeyPoints(points, 2);

        assertEquals(3, RouteEditUtils.countKeyRoutePoints(points));
        assertEquals(2, RouteEditUtils.countRemovableRoutePoints(points));
        assertEquals(3, result.size());
        assertEquals(0.0d, result.get(0).getBdLongitude(), 0.0000001d);
        assertEquals(0.001d, result.get(1).getBdLongitude(), 0.0000001d);
        assertEquals(0.0d, result.get(1).getBdLatitude(), 0.0000001d);
        assertEquals(0.001d, result.get(2).getBdLatitude(), 0.0000001d);
    }
}
