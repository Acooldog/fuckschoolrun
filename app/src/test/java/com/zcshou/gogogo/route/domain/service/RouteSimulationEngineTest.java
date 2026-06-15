package com.acooldog.toolbox.route.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.domain.model.RoutePoint;
import com.acooldog.toolbox.route.domain.model.RouteSimulationConfig;
import com.acooldog.toolbox.route.domain.model.SimulationFrame;

import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

public class RouteSimulationEngineTest {
    @Test
    public void next_usesCadenceFormulaWhenCadenceModeIsSelected() {
        RouteSimulationEngine engine = new RouteSimulationEngine(
                buildRoute(),
                new RouteSimulationConfig(RouteSimulationConfig.Mode.CADENCE, 0.0d, 120.0d, 1, false, 1000L)
        );

        SimulationFrame frame = engine.next();

        assertEquals(1.6f, frame.getSpeedMetersPerSecond(), 0.0001f);
        assertFalse(frame.isFinished());
    }

    @Test
    public void next_usesConfiguredSpeedWhenSpeedModeIsSelected() {
        RouteSimulationEngine engine = new RouteSimulationEngine(
                buildRoute(),
                new RouteSimulationConfig(RouteSimulationConfig.Mode.SPEED, 5.0d, 0.0d, 1, false, 1000L)
        );

        SimulationFrame frame = engine.next();

        assertEquals(5.0f, frame.getSpeedMetersPerSecond(), 0.0001f);
        assertFalse(frame.isFinished());
    }

    @Test
    public void next_appliesDynamicIntensityWithinConfiguredRange() {
        RouteSimulationEngine engine = new RouteSimulationEngine(
                buildRoute(),
                new RouteSimulationConfig(
                        RouteSimulationConfig.Mode.SPEED,
                        5.0d,
                        0.0d,
                        1,
                        true,
                        1.5d,
                        1.0d,
                        false,
                        0.0d,
                        1000L
                ),
                new SequenceRandom(0.9d)
        );

        SimulationFrame frame = engine.next();

        assertEquals(5.84f, frame.getSpeedMetersPerSecond(), 0.0001f);
        assertFalse(frame.isFinished());
    }

    @Test
    public void next_offsetsPointWhenNaturalPathVariationIsEnabled() {
        RouteSimulationEngine engine = new RouteSimulationEngine(
                buildRoute(),
                new RouteSimulationConfig(
                        RouteSimulationConfig.Mode.SPEED,
                        5.0d,
                        0.0d,
                        1,
                        false,
                        0.0d,
                        0.0d,
                        true,
                        2.0d,
                        1000L
                ),
                new SequenceRandom(0.9d)
        );
        RouteSimulationEngine baselineEngine = new RouteSimulationEngine(
                buildRoute(),
                new RouteSimulationConfig(RouteSimulationConfig.Mode.SPEED, 5.0d, 0.0d, 1, false, 1000L)
        );

        SimulationFrame frame = engine.next();
        SimulationFrame baselineFrame = baselineEngine.next();

        assertEquals(baselineFrame.getPoint().getWgsLongitude(), frame.getPoint().getWgsLongitude(), 0.0000001d);
        assertEquals(0.0d, baselineFrame.getPoint().getWgsLatitude(), 0.0000001d);
        assertEquals(0.288d, distanceMeters(baselineFrame.getPoint(), frame.getPoint()), 0.02d);
        assertFalse(frame.isFinished());
    }

    @Test
    public void next_offsetsAltitudeAroundConfiguredBaseAndHeightWhenNaturalAltitudeVariationIsEnabled() {
        RouteSimulationEngine engine = new RouteSimulationEngine(
                buildRoute(),
                new RouteSimulationConfig(
                        RouteSimulationConfig.Mode.SPEED,
                        5.0d,
                        0.0d,
                        1,
                        false,
                        0.0d,
                        0.0d,
                        false,
                        0.0d,
                        true,
                        55.0d,
                        0.5d,
                        180d,
                        1.0d,
                        1.0d,
                        1.0d,
                        1000L
                ),
                new SequenceRandom(1.0d)
        );
        RouteSimulationEngine baselineEngine = new RouteSimulationEngine(
                buildRoute(),
                new RouteSimulationConfig(RouteSimulationConfig.Mode.SPEED, 5.0d, 0.0d, 1, false, 1000L)
        );

        SimulationFrame frame = engine.next();
        SimulationFrame baselineFrame = baselineEngine.next();

        assertEquals(57.135d, frame.getPoint().getAltitude(), 0.0001d);
        assertEquals(0.0d, baselineFrame.getPoint().getAltitude(), 0.0001d);
        assertTrue(frame.getPoint().getAltitude() > baselineFrame.getPoint().getAltitude() + 56.8d);
        assertTrue(frame.getPoint().getAltitude() < baselineFrame.getPoint().getAltitude() + 57.3d);
        assertFalse(frame.isFinished());
    }

    @Test
    public void updateRoute_preservesProgressAndSwitchesToUpdatedGeometry() {
        RouteSimulationEngine engine = new RouteSimulationEngine(
                buildRoute(),
                new RouteSimulationConfig(RouteSimulationConfig.Mode.SPEED, 10.0d, 0.0d, 1, false, 1000L)
        );

        engine.next();
        engine.updateRoute(buildDiagonalRoute());
        SimulationFrame frame = engine.next();

        assertTrue(frame.getPoint().getWgsLatitude() > 0.0d);
        assertTrue(frame.getPoint().getWgsLongitude() > 0.0d);
        assertFalse(frame.isFinished());
    }

    @Test
    public void updateConfig_appliesReducedLoopCountWithoutResettingProgress() {
        RouteSimulationEngine engine = new RouteSimulationEngine(
                buildRoute(),
                new RouteSimulationConfig(RouteSimulationConfig.Mode.SPEED, 60.0d, 0.0d, 10, false, 1000L)
        );

        SimulationFrame frame = engine.next();
        while (frame.getCompletedLoops() < 1 && !frame.isFinished()) {
            frame = engine.next();
        }
        assertEquals(1, frame.getCompletedLoops());
        assertFalse(frame.isFinished());

        engine.updateConfig(new RouteSimulationConfig(RouteSimulationConfig.Mode.SPEED, 60.0d, 0.0d, 2, false, 1000L));

        SimulationFrame finishedFrame = engine.next();
        while (!finishedFrame.isFinished()) {
            finishedFrame = engine.next();
        }
        assertEquals(2, finishedFrame.getCompletedLoops());
        assertTrue(finishedFrame.isFinished());
    }

    private RouteDefinition buildRoute() {
        return new RouteDefinition(
                "route-test",
                "Route Test",
                0L,
                0L,
                Arrays.asList(
                        new RoutePoint(0.0d, 0.0d, 0.0d, 0.0d, 0.0d),
                        new RoutePoint(0.001d, 0.0d, 0.001d, 0.0d, 0.0d)
                ),
                null
        );
    }

    private RouteDefinition buildDiagonalRoute() {
        return new RouteDefinition(
                "route-diagonal",
                "Route Diagonal",
                0L,
                0L,
                Arrays.asList(
                        new RoutePoint(0.0d, 0.0d, 0.0d, 0.0d, 0.0d),
                        new RoutePoint(0.001d, 0.001d, 0.001d, 0.001d, 0.0d)
                ),
                null
        );
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

    private static final class SequenceRandom extends Random {
        private final double[] values;
        private int index;

        private SequenceRandom(double... values) {
            this.values = values == null ? new double[0] : values;
        }

        @Override
        public double nextDouble() {
            if (index >= values.length) {
                return values.length == 0 ? 0.5d : values[values.length - 1];
            }
            return values[index++];
        }
    }
}
