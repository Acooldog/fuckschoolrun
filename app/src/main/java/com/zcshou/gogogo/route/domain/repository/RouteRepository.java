package com.zcshou.gogogo.route.domain.repository;

import com.zcshou.gogogo.route.domain.model.RouteDefinition;
import com.zcshou.gogogo.route.domain.model.RoutePoint;
import com.zcshou.gogogo.route.domain.model.RouteShareInfo;

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
