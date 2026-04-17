package com.zcshou.gogogo.route.domain.usecase;

import com.zcshou.gogogo.route.domain.model.RouteDefinition;
import com.zcshou.gogogo.route.domain.model.RoutePoint;
import com.zcshou.gogogo.route.domain.model.RouteShareInfo;
import com.zcshou.gogogo.route.domain.repository.RouteRepository;

import java.io.IOException;
import java.util.List;

public final class SaveRouteUseCase {
    private final RouteRepository routeRepository;

    public SaveRouteUseCase(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public RouteDefinition execute(String routeName, List<RoutePoint> points) throws IOException {
        return routeRepository.saveRoute(routeName, points);
    }

    public RouteDefinition execute(String routeName, List<RoutePoint> points, RouteShareInfo shareInfo) throws IOException {
        return routeRepository.saveRoute(routeName, points, shareInfo);
    }
}
