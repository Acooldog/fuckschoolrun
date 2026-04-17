package com.zcshou.gogogo.route.presentation;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.zcshou.gogogo.route.domain.model.RouteDefinition;
import com.zcshou.gogogo.route.domain.model.RoutePoint;
import com.zcshou.gogogo.route.domain.model.RouteShareInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class RouteCreateViewModel extends AndroidViewModel {
    private final MutableLiveData<List<RoutePoint>> routePoints;
    private final RouteModule routeModule;

    public RouteCreateViewModel(@NonNull Application application) {
        super(application);
        routePoints = new MutableLiveData<>(new ArrayList<>());
        routeModule = RouteModule.from(application);
    }

    public LiveData<List<RoutePoint>> getRoutePoints() {
        return routePoints;
    }

    public void addPoint(RoutePoint routePoint) {
        List<RoutePoint> updatedPoints = new ArrayList<>(getCurrentPoints());
        updatedPoints.add(routePoint);
        routePoints.setValue(updatedPoints);
    }

    public void clear() {
        routePoints.setValue(new ArrayList<>());
    }

    public RouteDefinition saveRoute(String routeName) throws IOException {
        return routeModule.saveRouteUseCase().execute(routeName, getCurrentPoints());
    }

    public RouteDefinition saveRoute(String routeName, List<RoutePoint> points, RouteShareInfo shareInfo) throws IOException {
        return routeModule.saveRouteUseCase().execute(routeName, points, shareInfo);
    }

    public boolean canSave() {
        return getCurrentPoints().size() >= 2;
    }

    public List<RoutePoint> getCurrentPoints() {
        List<RoutePoint> current = routePoints.getValue();
        if (current == null) {
            return new ArrayList<>();
        }
        return current;
    }
}
