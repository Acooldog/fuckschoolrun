package com.acooldog.toolbox.route.domain.usecase;

import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.domain.model.RoutePoint;
import com.acooldog.toolbox.route.domain.model.RouteShareInfo;
import com.acooldog.toolbox.route.domain.repository.RouteRepository;

import java.io.IOException;
import java.util.List;

public final class UpdateRouteUseCase {
    private final RouteRepository routeRepository;

    public UpdateRouteUseCase(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public RouteDefinition execute(
            String routeId,
            String routeName,
            List<RoutePoint> points,
            RouteShareInfo shareInfo
    ) throws IOException {
        return routeRepository.updateRoute(routeId, routeName, points, shareInfo);
    }
}
