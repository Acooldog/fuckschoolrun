package com.acooldog.toolbox.route.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.domain.model.RoutePoint;
import com.acooldog.toolbox.route.domain.model.RouteSimulationConfig;
import com.acooldog.toolbox.route.domain.model.SimulationFrame;

import org.junit.Test;

import java.util.Arrays;

public class RouteSimulationEngineTest {
    @Test
    public void next_usesCadenceFormulaWhenCadenceModeIsSelected() {
        RouteSimulationEngine engine = new RouteSimulationEngine(
                buildRoute(),
                new RouteSimulationConfig(RouteSimulationConfig.Mode.CADENCE, 0.0d, 120.0d, 1, false, 1000L)
        );

        SimulationFrame frame = engine.next(1.0d);

        assertEquals(1.6f, frame.getSpeedMetersPerSecond(), 0.0001f);
        assertFalse(frame.isFinished());
    }

    @Test
    public void next_usesConfiguredSpeedWhenSpeedModeIsSelected() {
        RouteSimulationEngine engine = new RouteSimulationEngine(
                buildRoute(),
                new RouteSimulationConfig(RouteSimulationConfig.Mode.SPEED, 5.0d, 0.0d, 1, false, 1000L)
        );

        SimulationFrame frame = engine.next(1.0d);

        assertEquals(5.0f, frame.getSpeedMetersPerSecond(), 0.0001f);
        assertFalse(frame.isFinished());
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
}
