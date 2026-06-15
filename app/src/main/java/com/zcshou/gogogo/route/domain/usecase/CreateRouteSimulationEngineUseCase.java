package com.acooldog.toolbox.route.domain.usecase;

import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.domain.model.RouteSimulationConfig;
import com.acooldog.toolbox.route.domain.service.RouteSimulationEngine;

public final class CreateRouteSimulationEngineUseCase {
    public RouteSimulationEngine execute(RouteDefinition routeDefinition, RouteSimulationConfig config) {
        return new RouteSimulationEngine(routeDefinition, config);
    }
}
