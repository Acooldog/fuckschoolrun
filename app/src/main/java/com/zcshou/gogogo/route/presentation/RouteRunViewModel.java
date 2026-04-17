package com.zcshou.gogogo.route.presentation;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.zcshou.gogogo.route.domain.model.RouteDefinition;
import com.zcshou.gogogo.route.domain.model.RoutePoint;
import com.zcshou.gogogo.route.domain.model.RouteSimulationConfig;
import com.zcshou.gogogo.route.domain.model.RouteShareInfo;
import com.zcshou.gogogo.route.domain.model.SimulationFrame;
import com.zcshou.gogogo.route.domain.service.LocationSimulationGateway;
import com.zcshou.gogogo.route.domain.service.RouteSimulationEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RouteRunViewModel extends AndroidViewModel {
    private final MutableLiveData<List<RouteDefinition>> routes;
    private final MutableLiveData<RouteDefinition> selectedRoute;
    private final MutableLiveData<SimulationFrame> simulationFrame;
    private final MutableLiveData<Boolean> running;
    private final Handler handler;
    private final Random random;
    private final RouteModule routeModule;
    private RouteSimulationEngine simulationEngine;
    private LocationSimulationGateway locationSimulationGateway;
    private RouteSimulationConfig simulationConfig;

    private final Runnable simulationRunnable = new Runnable() {
        @Override
        public void run() {
            if (simulationEngine == null || simulationConfig == null || locationSimulationGateway == null) {
                stopSimulation();
                return;
            }

            double speedMultiplier = simulationConfig.isSpeedFloating()
                    ? 0.85d + (random.nextDouble() * 0.30d)
                    : 1.0d;
            SimulationFrame nextFrame = simulationEngine.next(speedMultiplier);
            simulationFrame.setValue(nextFrame);
            locationSimulationGateway.pushLocation(
                    nextFrame.getPoint().getWgsLongitude(),
                    nextFrame.getPoint().getWgsLatitude(),
                    nextFrame.getPoint().getAltitude(),
                    nextFrame.getSpeedMetersPerSecond(),
                    nextFrame.getBearing()
            );

            if (nextFrame.isFinished()) {
                stopSimulation();
                return;
            }
            handler.postDelayed(this, simulationConfig.getTickMillis());
        }
    };

    public RouteRunViewModel(@NonNull Application application) {
        super(application);
        routes = new MutableLiveData<>(new ArrayList<>());
        selectedRoute = new MutableLiveData<>();
        simulationFrame = new MutableLiveData<>();
        running = new MutableLiveData<>(false);
        handler = new Handler(Looper.getMainLooper());
        random = new Random();
        routeModule = RouteModule.from(application);
    }

    public LiveData<List<RouteDefinition>> getRoutes() {
        return routes;
    }

    public LiveData<RouteDefinition> getSelectedRoute() {
        return selectedRoute;
    }

    public LiveData<SimulationFrame> getSimulationFrame() {
        return simulationFrame;
    }

    public LiveData<Boolean> isRunning() {
        return running;
    }

    public void refreshRoutes() throws IOException {
        routes.setValue(routeModule.getRoutesUseCase().execute());
    }

    public void selectRoute(RouteDefinition routeDefinition) {
        selectedRoute.setValue(routeDefinition);
    }

    public void selectRouteById(String routeId) throws IOException {
        for (RouteDefinition routeDefinition : routeModule.getRoutesUseCase().execute()) {
            if (routeDefinition.getId().equals(routeId)) {
                selectedRoute.setValue(routeDefinition);
                break;
            }
        }
    }

    public RouteDefinition saveRoute(String routeName, List<RoutePoint> points, RouteShareInfo shareInfo) throws IOException {
        RouteDefinition routeDefinition = routeModule.saveRouteUseCase().execute(routeName, points, shareInfo);
        routes.postValue(routeModule.getRoutesUseCase().execute());
        selectedRoute.postValue(routeDefinition);
        return routeDefinition;
    }

    public void startSimulation(RouteSimulationConfig config, LocationSimulationGateway gateway) {
        RouteDefinition routeDefinition = selectedRoute.getValue();
        if (routeDefinition == null) {
            throw new IllegalStateException("No route selected");
        }
        simulationConfig = config;
        locationSimulationGateway = gateway;
        simulationEngine = routeModule.createRouteSimulationEngineUseCase().execute(routeDefinition, config);
        running.setValue(true);
        handler.removeCallbacks(simulationRunnable);
        simulationRunnable.run();
    }

    public void stopSimulation() {
        handler.removeCallbacks(simulationRunnable);
        simulationEngine = null;
        locationSimulationGateway = null;
        simulationConfig = null;
        running.setValue(false);
    }

    @Override
    protected void onCleared() {
        stopSimulation();
        super.onCleared();
    }
}
