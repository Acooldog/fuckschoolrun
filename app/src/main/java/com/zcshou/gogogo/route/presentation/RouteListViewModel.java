package com.zcshou.gogogo.route.presentation;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.zcshou.gogogo.route.domain.model.RouteDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class RouteListViewModel extends AndroidViewModel {
    private final MutableLiveData<List<RouteDefinition>> routes;
    private final RouteModule routeModule;

    public RouteListViewModel(@NonNull Application application) {
        super(application);
        routes = new MutableLiveData<>(new ArrayList<>());
        routeModule = RouteModule.from(application);
    }

    public LiveData<List<RouteDefinition>> getRoutes() {
        return routes;
    }

    public void refresh() throws IOException {
        routes.setValue(routeModule.getRoutesUseCase().execute());
    }

    public void deleteRoute(RouteDefinition routeDefinition) throws IOException {
        routeModule.deleteRouteUseCase().execute(routeDefinition.getId());
        refresh();
    }

    public RouteDefinition importRoute(Uri uri) throws IOException {
        String fileName = uri.getLastPathSegment();
        try (InputStream inputStream = getApplication().getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException("Unable to open route file");
            }
            RouteDefinition routeDefinition = routeModule.importRouteUseCase().execute(fileName, inputStream);
            refresh();
            return routeDefinition;
        }
    }
}
