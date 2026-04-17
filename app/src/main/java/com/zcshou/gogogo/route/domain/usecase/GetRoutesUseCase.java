package com.zcshou.gogogo.route.domain.usecase;

import com.zcshou.gogogo.route.domain.model.RouteDefinition;
import com.zcshou.gogogo.route.domain.repository.RouteRepository;

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
