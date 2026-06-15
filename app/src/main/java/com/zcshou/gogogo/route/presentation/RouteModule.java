package com.acooldog.toolbox.route.presentation;

import android.content.Context;

import com.acooldog.toolbox.route.data.FileRouteRepository;
import com.acooldog.toolbox.route.domain.repository.RouteRepository;
import com.acooldog.toolbox.route.domain.usecase.CreateRouteSimulationEngineUseCase;
import com.acooldog.toolbox.route.domain.usecase.DeleteRouteUseCase;
import com.acooldog.toolbox.route.domain.usecase.GetRoutesUseCase;
import com.acooldog.toolbox.route.domain.usecase.ImportRouteUseCase;
import com.acooldog.toolbox.route.domain.usecase.SaveRouteUseCase;
import com.acooldog.toolbox.route.domain.usecase.UpdateRouteUseCase;

public final class RouteModule {
    private static volatile RouteModule instance;

    private final GetRoutesUseCase getRoutesUseCase;
    private final SaveRouteUseCase saveRouteUseCase;
    private final UpdateRouteUseCase updateRouteUseCase;
    private final ImportRouteUseCase importRouteUseCase;
    private final DeleteRouteUseCase deleteRouteUseCase;
    private final CreateRouteSimulationEngineUseCase createRouteSimulationEngineUseCase;

    private RouteModule(Context context) {
        RouteRepository routeRepository = new FileRouteRepository(context.getApplicationContext());
        getRoutesUseCase = new GetRoutesUseCase(routeRepository);
        saveRouteUseCase = new SaveRouteUseCase(routeRepository);
        updateRouteUseCase = new UpdateRouteUseCase(routeRepository);
        importRouteUseCase = new ImportRouteUseCase(routeRepository);
        deleteRouteUseCase = new DeleteRouteUseCase(routeRepository);
        createRouteSimulationEngineUseCase = new CreateRouteSimulationEngineUseCase();
    }

    public static RouteModule from(Context context) {
        if (instance == null) {
            synchronized (RouteModule.class) {
                if (instance == null) {
                    instance = new RouteModule(context);
                }
            }
        }
        return instance;
    }

    public GetRoutesUseCase getRoutesUseCase() {
        return getRoutesUseCase;
    }

    public SaveRouteUseCase saveRouteUseCase() {
        return saveRouteUseCase;
    }

    public UpdateRouteUseCase updateRouteUseCase() {
        return updateRouteUseCase;
    }

    public ImportRouteUseCase importRouteUseCase() {
        return importRouteUseCase;
    }

    public DeleteRouteUseCase deleteRouteUseCase() {
        return deleteRouteUseCase;
    }

    public CreateRouteSimulationEngineUseCase createRouteSimulationEngineUseCase() {
        return createRouteSimulationEngineUseCase;
    }
}
