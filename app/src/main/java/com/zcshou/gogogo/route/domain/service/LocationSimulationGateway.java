package com.acooldog.toolbox.route.domain.service;

public interface LocationSimulationGateway {
    void pushLocation(double longitude, double latitude, double altitude, float speed, float bearing);
}
