package com.zcshou.gogogo.route.domain.usecase;

import com.zcshou.gogogo.route.domain.model.RouteDefinition;
import com.zcshou.gogogo.route.domain.repository.RouteRepository;

import java.io.IOException;
import java.io.InputStream;

public final class ImportRouteUseCase {
    private final RouteRepository routeRepository;

    public ImportRouteUseCase(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public RouteDefinition execute(String displayName, InputStream inputStream) throws IOException {
        return routeRepository.importRoute(displayName, inputStream);
    }
}
