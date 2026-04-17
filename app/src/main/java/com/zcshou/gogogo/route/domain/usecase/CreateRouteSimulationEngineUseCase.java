package com.zcshou.gogogo.route.domain.usecase;

import com.zcshou.gogogo.route.domain.model.RouteDefinition;
import com.zcshou.gogogo.route.domain.model.RouteSimulationConfig;
import com.zcshou.gogogo.route.domain.service.RouteSimulationEngine;

public final class CreateRouteSimulationEngineUseCase {
    public RouteSimulationEngine execute(RouteDefinition routeDefinition, RouteSimulationConfig config) {
        return new RouteSimulationEngine(routeDefinition, config);
    }
}
