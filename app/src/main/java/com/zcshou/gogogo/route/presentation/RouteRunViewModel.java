package com.acooldog.toolbox.route.presentation;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.domain.model.RoutePoint;
import com.acooldog.toolbox.route.domain.model.RouteSimulationConfig;
import com.acooldog.toolbox.route.domain.model.RouteShareInfo;
import com.acooldog.toolbox.route.domain.model.SimulationFrame;
import com.acooldog.toolbox.route.domain.service.LocationSimulationGateway;
import com.acooldog.toolbox.route.domain.service.RouteSimulationEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class RouteRunViewModel extends AndroidViewModel {
    private final MutableLiveData<List<RouteDefinition>> routes;
    private final MutableLiveData<RouteDefinition> selectedRoute;
    private final MutableLiveData<SimulationFrame> simulationFrame;
    private final MutableLiveData<Long> simulationCompletedEvent;
    private final MutableLiveData<Boolean> resumable;
    private final MutableLiveData<Boolean> running;
    private final Handler handler;
    private final RouteModule routeModule;
    private RouteSimulationEngine simulationEngine;
    private LocationSimulationGateway locationSimulationGateway;
    private RouteSimulationConfig simulationConfig;
    private String activeRouteId;

    private final Runnable simulationRunnable = new Runnable() {
        @Override
        public void run() {
            if (!canSimulate()) {
                running.setValue(false);
                updateSimulationAvailability();
                return;
            }

            SimulationFrame frame = advanceSimulationOnceAndGetFrame();
            if (frame == null || frame.isFinished() || simulationConfig == null || !Boolean.TRUE.equals(running.getValue())) {
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
        simulationCompletedEvent = new MutableLiveData<>();
        resumable = new MutableLiveData<>(false);
        running = new MutableLiveData<>(false);
        handler = new Handler(Looper.getMainLooper());
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

    public LiveData<Long> getSimulationCompletedEvent() {
        return simulationCompletedEvent;
    }

    public LiveData<Boolean> isResumable() {
        return resumable;
    }

    public LiveData<Boolean> isRunning() {
        return running;
    }

    public void refreshRoutes() throws IOException {
        routes.setValue(routeModule.getRoutesUseCase().execute());
    }

    public void selectRoute(RouteDefinition routeDefinition) {
        selectedRoute.setValue(routeDefinition);
        updateRouteInCollection(routeDefinition);
        updateSimulationAvailability();
    }

    public void selectRouteById(String routeId) throws IOException {
        for (RouteDefinition routeDefinition : routeModule.getRoutesUseCase().execute()) {
            if (routeDefinition.getId().equals(routeId)) {
                selectedRoute.setValue(routeDefinition);
                updateSimulationAvailability();
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
        prepareSimulation(config, gateway);
        handler.removeCallbacks(simulationRunnable);
        simulationRunnable.run();
    }

    public void startLinkedSimulation(RouteSimulationConfig config, LocationSimulationGateway gateway) {
        prepareSimulation(config, gateway);
        handler.removeCallbacks(simulationRunnable);
    }

    public SimulationFrame advanceSimulationOnceAndGetFrame() {
        if (!canSimulate()) {
            running.setValue(false);
            updateSimulationAvailability();
            return null;
        }
        if (simulationEngine == null || simulationConfig == null) {
            stopSimulation();
            return null;
        }
        SimulationFrame nextFrame = simulationEngine.next();
        simulationFrame.setValue(nextFrame);
        locationSimulationGateway.pushLocation(
                nextFrame.getPoint().getWgsLongitude(),
                nextFrame.getPoint().getWgsLatitude(),
                nextFrame.getPoint().getAltitude(),
                nextFrame.getSpeedMetersPerSecond(),
                nextFrame.getBearing()
        );
        if (nextFrame.isFinished()) {
            simulationCompletedEvent.setValue(SystemClock.elapsedRealtime());
            stopSimulation();
            return nextFrame;
        }
        return nextFrame;
    }

    public boolean advanceSimulationOnce() {
        return advanceSimulationOnceAndGetFrame() != null;
    }

    public void pauseSimulation() {
        handler.removeCallbacks(simulationRunnable);
        locationSimulationGateway = null;
        running.setValue(false);
        updateSimulationAvailability();
    }

    public void updateSimulationConfig(RouteSimulationConfig config) {
        if (config == null || simulationEngine == null) {
            return;
        }
        simulationConfig = config;
        simulationEngine.updateConfig(config);
        if (simulationEngine.isFinished()) {
            simulationCompletedEvent.setValue(SystemClock.elapsedRealtime());
            stopSimulation();
            return;
        }
        if (Boolean.TRUE.equals(running.getValue())) {
            handler.removeCallbacks(simulationRunnable);
            handler.postDelayed(simulationRunnable, simulationConfig.getTickMillis());
        }
        updateSimulationAvailability();
    }

    public void replaceSelectedRoute(RouteDefinition routeDefinition) {
        if (routeDefinition == null) {
            return;
        }
        selectedRoute.setValue(routeDefinition);
        updateRouteInCollection(routeDefinition);
        if (simulationEngine != null
                && activeRouteId != null
                && activeRouteId.equals(routeDefinition.getId())) {
            simulationEngine.updateRoute(routeDefinition);
        }
        updateSimulationAvailability();
    }

    public RouteDefinition persistRouteEdits(RouteDefinition routeDefinition) throws IOException {
        return routeModule.updateRouteUseCase().execute(
                routeDefinition.getId(),
                routeDefinition.getName(),
                routeDefinition.getPoints(),
                routeDefinition.getShareInfo()
        );
    }

    public boolean hasActiveSimulation() {
        return simulationEngine != null && !simulationEngine.isFinished();
    }

    public boolean hasResumableSimulationForSelectedRoute() {
        return hasActiveSimulation()
                && !Boolean.TRUE.equals(running.getValue())
                && isSelectedRouteActive();
    }

    public void stopSimulation() {
        handler.removeCallbacks(simulationRunnable);
        simulationEngine = null;
        activeRouteId = null;
        locationSimulationGateway = null;
        simulationConfig = null;
        running.setValue(false);
        updateSimulationAvailability();
    }

    @Override
    protected void onCleared() {
        stopSimulation();
        super.onCleared();
    }

    private boolean canSimulate() {
        return simulationEngine != null && simulationConfig != null && locationSimulationGateway != null;
    }

    private void prepareSimulation(RouteSimulationConfig config, LocationSimulationGateway gateway) {
        RouteDefinition routeDefinition = selectedRoute.getValue();
        if (routeDefinition == null) {
            throw new IllegalStateException("No route selected");
        }
        simulationConfig = config;
        locationSimulationGateway = gateway;
        if (simulationEngine != null
                && !simulationEngine.isFinished()
                && routeDefinition.getId().equals(activeRouteId)) {
            simulationEngine.updateConfig(config);
        } else {
            simulationEngine = routeModule.createRouteSimulationEngineUseCase().execute(routeDefinition, config);
            activeRouteId = routeDefinition.getId();
        }
        running.setValue(true);
        updateSimulationAvailability();
    }

    private boolean isSelectedRouteActive() {
        RouteDefinition routeDefinition = selectedRoute.getValue();
        return routeDefinition != null
                && activeRouteId != null
                && activeRouteId.equals(routeDefinition.getId());
    }

    private void updateSimulationAvailability() {
        resumable.setValue(
                simulationEngine != null
                        && !simulationEngine.isFinished()
                        && !Boolean.TRUE.equals(running.getValue())
                        && isSelectedRouteActive()
        );
    }

    private void updateRouteInCollection(RouteDefinition routeDefinition) {
        if (routeDefinition == null) {
            return;
        }
        List<RouteDefinition> currentRoutes = routes.getValue();
        if (currentRoutes == null || currentRoutes.isEmpty()) {
            return;
        }
        List<RouteDefinition> updatedRoutes = new ArrayList<>(currentRoutes);
        for (int index = 0; index < updatedRoutes.size(); index++) {
            RouteDefinition existing = updatedRoutes.get(index);
            if (existing != null && routeDefinition.getId().equals(existing.getId())) {
                updatedRoutes.set(index, routeDefinition);
                routes.setValue(updatedRoutes);
                return;
            }
        }
    }
}
