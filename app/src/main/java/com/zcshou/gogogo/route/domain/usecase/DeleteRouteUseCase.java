package com.zcshou.gogogo.route.domain.usecase;

import com.zcshou.gogogo.route.domain.repository.RouteRepository;

import java.io.IOException;

public final class DeleteRouteUseCase {
    private final RouteRepository routeRepository;

    public DeleteRouteUseCase(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public void execute(String routeId) throws IOException {
        routeRepository.deleteRoute(routeId);
    }
}
