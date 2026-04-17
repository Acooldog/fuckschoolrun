package com.acooldog.toolbox.route.domain.usecase;

import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.domain.repository.RouteRepository;

import java.io.IOException;
import java.util.List;

public final class GetRoutesUseCase {
    private final RouteRepository routeRepository;

    public GetRoutesUseCase(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public List<RouteDefinition> execute() throws IOException {
        return routeRepository.getRoutes();
    }
}
