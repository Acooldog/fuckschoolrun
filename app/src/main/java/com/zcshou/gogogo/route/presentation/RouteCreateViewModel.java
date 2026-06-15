package com.acooldog.toolbox.route.presentation;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.domain.model.RoutePoint;
import com.acooldog.toolbox.route.domain.model.RouteShareInfo;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class RouteCreateViewModel extends AndroidViewModel {
    private final MutableLiveData<List<RoutePoint>> routePoints;
    private final RouteModule routeModule;
    private final ArrayDeque<List<RoutePoint>> undoStack;
    private final ArrayDeque<List<RoutePoint>> redoStack;

    public RouteCreateViewModel(@NonNull Application application) {
        super(application);
        routePoints = new MutableLiveData<>(new ArrayList<>());
        routeModule = RouteModule.from(application);
        undoStack = new ArrayDeque<>();
        redoStack = new ArrayDeque<>();
    }

    public LiveData<List<RoutePoint>> getRoutePoints() {
        return routePoints;
    }

    public void addPoint(RoutePoint routePoint) {
        addPoint(routePoint, true);
    }

    public void addPoint(RoutePoint routePoint, boolean recordHistory) {
        if (recordHistory) {
            pushHistory();
        }
        List<RoutePoint> updatedPoints = new ArrayList<>(getCurrentPoints());
        updatedPoints.add(routePoint);
        routePoints.setValue(updatedPoints);
    }

    public void setPoints(List<RoutePoint> points) {
        pushHistory();
        routePoints.setValue(points == null ? new ArrayList<>() : new ArrayList<>(points));
    }

    public void loadPoints(List<RoutePoint> points) {
        undoStack.clear();
        redoStack.clear();
        routePoints.setValue(points == null ? new ArrayList<>() : new ArrayList<>(points));
    }

    public void removePointAt(int index) {
        List<RoutePoint> updatedPoints = new ArrayList<>(getCurrentPoints());
        if (index < 0 || index >= updatedPoints.size()) {
            return;
        }
        pushHistory();
        updatedPoints.remove(index);
        routePoints.setValue(updatedPoints);
    }

    public void replacePointAt(int index, RoutePoint routePoint) {
        List<RoutePoint> updatedPoints = new ArrayList<>(getCurrentPoints());
        if (index < 0 || index >= updatedPoints.size() || routePoint == null) {
            return;
        }
        pushHistory();
        updatedPoints.set(index, routePoint);
        routePoints.setValue(updatedPoints);
    }

    public void clear() {
        pushHistory();
        routePoints.setValue(new ArrayList<>());
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        redoStack.push(new ArrayList<>(getCurrentPoints()));
        routePoints.setValue(undoStack.pop());
    }

    public void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        undoStack.push(new ArrayList<>(getCurrentPoints()));
        routePoints.setValue(redoStack.pop());
    }

    public RouteDefinition saveRoute(String routeName) throws IOException {
        return routeModule.saveRouteUseCase().execute(routeName, getCurrentPoints());
    }

    public RouteDefinition saveRoute(String routeName, List<RoutePoint> points, RouteShareInfo shareInfo) throws IOException {
        return routeModule.saveRouteUseCase().execute(routeName, points, shareInfo);
    }

    public RouteDefinition updateRoute(String routeId, String routeName, List<RoutePoint> points, RouteShareInfo shareInfo) throws IOException {
        return routeModule.updateRouteUseCase().execute(routeId, routeName, points, shareInfo);
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

    private void pushHistory() {
        undoStack.push(new ArrayList<>(getCurrentPoints()));
        redoStack.clear();
    }
}
