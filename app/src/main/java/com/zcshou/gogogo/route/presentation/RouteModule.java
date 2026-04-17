package com.zcshou.gogogo.route.presentation;

import android.content.Context;

import com.zcshou.gogogo.route.data.FileRouteRepository;
import com.zcshou.gogogo.route.domain.repository.RouteRepository;
import com.zcshou.gogogo.route.domain.usecase.CreateRouteSimulationEngineUseCase;
import com.zcshou.gogogo.route.domain.usecase.DeleteRouteUseCase;
import com.zcshou.gogogo.route.domain.usecase.GetRoutesUseCase;
import com.zcshou.gogogo.route.domain.usecase.ImportRouteUseCase;
import com.zcshou.gogogo.route.domain.usecase.SaveRouteUseCase;

public final class RouteModule {
    private static volatile RouteModule instance;

    private final GetRoutesUseCase getRoutesUseCase;
    private final SaveRouteUseCase saveRouteUseCase;
    private final ImportRouteUseCase importRouteUseCase;
    private final DeleteRouteUseCase deleteRouteUseCase;
    private final CreateRouteSimulationEngineUseCase createRouteSimulationEngineUseCase;

    private RouteModule(Context context) {
        RouteRepository routeRepository = new FileRouteRepository(context.getApplicationContext());
        getRoutesUseCase = new GetRoutesUseCase(routeRepository);
        saveRouteUseCase = new SaveRouteUseCase(routeRepository);
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
