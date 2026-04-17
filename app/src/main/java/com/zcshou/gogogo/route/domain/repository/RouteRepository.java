package com.acooldog.toolbox.route.domain.repository;

import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.domain.model.RoutePoint;
import com.acooldog.toolbox.route.domain.model.RouteShareInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface RouteRepository {
    List<RouteDefinition> getRoutes() throws IOException;

    RouteDefinition getRoute(String routeId) throws IOException;

    RouteDefinition saveRoute(String routeName, List<RoutePoint> points) throws IOException;

    RouteDefinition saveRoute(String routeName, List<RoutePoint> points, RouteShareInfo shareInfo) throws IOException;

    RouteDefinition importRoute(String displayName, InputStream inputStream) throws IOException;

    void deleteRoute(String routeId) throws IOException;
}
