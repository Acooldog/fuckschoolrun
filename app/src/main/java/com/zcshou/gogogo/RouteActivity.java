package com.acooldog.toolbox;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;

import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.presentation.LocalRouteActionAdapter;
import com.acooldog.toolbox.route.presentation.RouteListViewModel;
import com.acooldog.toolbox.route.presentation.RouteModule;
import com.acooldog.toolbox.route.presentation.SharedRouteActionAdapter;
import com.acooldog.toolbox.share.domain.model.SharedRoutePayload;
import com.acooldog.toolbox.share.domain.model.SharedRouteSummary;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.utils.GoUtils;
import com.acooldog.toolbox.utils.SearchSortUtils;
import com.acooldog.toolbox.utils.ShareUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RouteActivity extends BaseActivity {
    private static final int TAB_LOCAL = 0;
    private static final int TAB_SHARED = 1;

    private RouteListViewModel viewModel;
    private LocalRouteActionAdapter localRouteAdapter;
    private SharedRouteActionAdapter sharedRouteAdapter;
    private TextView emptyView;
    private ListView routeListView;
    private FloatingActionButton importButton;
    private ExecutorService ioExecutor;
    private int currentTab = TAB_LOCAL;
    private List<SharedRouteSummary> sharedRoutes = new ArrayList<>();
    private String currentQuery = "";

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleImportedRoute);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(RouteListViewModel.class);
        ioExecutor = Executors.newSingleThreadExecutor();
        localRouteAdapter = new LocalRouteActionAdapter(this, new LocalActions());
        sharedRouteAdapter = new SharedRouteActionAdapter(this, new SharedActions());

        emptyView = findViewById(R.id.route_no_data);
        routeListView = findViewById(R.id.route_list_view);
        importButton = findViewById(R.id.fab_import);
        importButton.setOnClickListener(v -> importLauncher.launch(new String[]{"application/json", "*/*"}));
        SearchView searchView = findViewById(R.id.route_search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query == null ? "" : query.trim();
                renderCurrentTab();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText == null ? "" : newText.trim();
                renderCurrentTab();
                return true;
            }
        });

        TabLayout tabLayout = findViewById(R.id.route_tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.route_tab_local));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.route_tab_shared));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab == null ? TAB_LOCAL : tab.getPosition();
                renderCurrentTab();
                if (currentTab == TAB_SHARED) {
                    loadSharedRoutes();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // No-op.
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab != null && tab.getPosition() == TAB_SHARED) {
                    loadSharedRoutes();
                } else {
                    refreshRoutes();
                }
            }
        });

        observeRoutes();
        renderCurrentTab();
        refreshRoutes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRoutes();
    }

    @Override
    protected void onDestroy() {
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void observeRoutes() {
        viewModel.getRoutes().observe(this, routes -> {
            renderCurrentTab();
        });
    }

    private void renderCurrentTab() {
        boolean localTab = currentTab == TAB_LOCAL;
        routeListView.setAdapter(localTab ? localRouteAdapter : sharedRouteAdapter);
        importButton.setVisibility(localTab ? View.VISIBLE : View.GONE);
        if (localTab) {
            List<RouteDefinition> routes = filterLocalRoutes(viewModel.getRoutes().getValue(), currentQuery);
            localRouteAdapter.submit(routes);
            updateEmptyState(routes.isEmpty(), R.string.route_empty_local);
        } else {
            List<SharedRouteSummary> routes = filterSharedRoutes(sharedRoutes, currentQuery);
            sharedRouteAdapter.submit(routes);
            updateEmptyState(routes.isEmpty(), R.string.route_empty_shared);
        }
    }

    private void updateEmptyState(boolean isEmpty, int messageResId) {
        emptyView.setText(messageResId);
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        routeListView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void refreshRoutes() {
        try {
            viewModel.refresh();
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, "读取路线失败");
        }
    }

    private void handleImportedRoute(Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            viewModel.importRoute(uri);
            GoUtils.DisplayToast(this, "路线导入成功");
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, "路线导入失败");
        }
    }

    private void openRouteForSimulation(RouteDefinition routeDefinition) {
        Intent intent = new Intent(this, RouteRunActivity.class);
        intent.putExtra(RouteRunActivity.EXTRA_ROUTE_ID, routeDefinition.getId());
        startActivity(intent);
    }

    private void openSharedRouteForSimulation(SharedRouteSummary summary) {
        Intent intent = new Intent(this, RouteRunActivity.class);
        intent.putExtra(RouteRunActivity.EXTRA_SHARED_ROUTE_ID, summary.getId());
        intent.putExtra(RouteRunActivity.EXTRA_SHARED_ROUTE_NAME, summary.getName());
        startActivity(intent);
    }

    private void openRouteForEditing(RouteDefinition routeDefinition) {
        Intent intent = new Intent(this, RouteCreateActivity.class);
        intent.putExtra(RouteCreateActivity.EXTRA_EDIT_ROUTE_ID, routeDefinition.getId());
        startActivity(intent);
    }

    private void loadSharedRoutes() {
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_network));
            updateEmptyState(sharedRoutes.isEmpty(), R.string.route_empty_shared);
            return;
        }
        GoUtils.DisplayToast(this, getString(R.string.route_shared_loading));
        ioExecutor.execute(() -> {
            try {
                List<SharedRouteSummary> routes = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getSharedRoutes();
                sharedRoutes = routes == null ? new ArrayList<>() : routes;
                sharedRoutes.sort(Comparator.comparing(route -> SearchSortUtils.buildSortKey(route.getName())));
                runOnUiThread(() -> {
                    renderCurrentTab();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.route_shared_load_failed, exception)));
            }
        });
    }

    private void downloadSharedRoute(SharedRouteSummary summary, boolean startSimulationAfterDownload) {
        if (summary == null) {
            return;
        }
        if (summary.isPrivacyMode()) {
            if (startSimulationAfterDownload) {
                GoUtils.DisplayToast(this, getString(R.string.route_privacy_simulation_loaded));
                openSharedRouteForSimulation(summary);
            } else {
                GoUtils.DisplayToast(this, getString(R.string.route_privacy_download_blocked));
            }
            return;
        }
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_network));
            return;
        }
        GoUtils.DisplayToast(this, getString(R.string.route_shared_downloading));
        ioExecutor.execute(() -> {
            try {
                SharedRoutePayload payload = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getSharedRoute(summary.getId());
                String localName = payload.getName().isEmpty() ? summary.getName() : payload.getName();
                RouteDefinition routeDefinition = RouteModule.from(getApplicationContext())
                        .saveRouteUseCase()
                        .execute(localName, payload.getPoints(), payload.toShareInfo(true));
                runOnUiThread(() -> {
                    GoUtils.DisplayToast(
                            this,
                            getString(payload.isPrivacyMode() ? R.string.route_shared_downloaded_private : R.string.route_shared_downloaded)
                    );
                    refreshRoutes();
                    if (startSimulationAfterDownload) {
                        openRouteForSimulation(routeDefinition);
                    }
                });
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.route_shared_download_failed, exception)));
            }
        });
    }

    private void confirmDelete(RouteDefinition routeDefinition) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.route_delete_title)
                .setMessage(R.string.route_delete_message)
                .setPositiveButton(R.string.route_item_delete, (dialog, which) -> {
                    try {
                        viewModel.deleteRoute(routeDefinition);
                        GoUtils.DisplayToast(this, getString(R.string.route_delete_success));
                    } catch (Exception exception) {
                        GoUtils.DisplayToast(this, getString(R.string.route_delete_failed));
                    }
                })
                .setNegativeButton(R.string.route_share_cancel, null)
                .show();
    }

    private String buildDetailedToast(int prefixResId, Exception exception) {
        String detail = exception == null || exception.getMessage() == null ? "" : exception.getMessage().trim();
        if (detail.isEmpty()) {
            return getString(prefixResId);
        }
        return getString(prefixResId) + " " + detail;
    }

    private List<RouteDefinition> filterLocalRoutes(@Nullable List<RouteDefinition> routes, @NonNull String query) {
        List<RouteDefinition> filtered = new ArrayList<>();
        if (routes == null) {
            return filtered;
        }
        for (RouteDefinition routeDefinition : routes) {
            if (SearchSortUtils.matches(query, routeDefinition.getName())) {
                filtered.add(routeDefinition);
            }
        }
        return filtered;
    }

    private List<SharedRouteSummary> filterSharedRoutes(@Nullable List<SharedRouteSummary> routes, @NonNull String query) {
        List<SharedRouteSummary> filtered = new ArrayList<>();
        if (routes == null) {
            return filtered;
        }
        for (SharedRouteSummary route : routes) {
            if (SearchSortUtils.matches(query, route.getName())) {
                filtered.add(route);
            }
        }
        return filtered;
    }

    private final class LocalActions implements LocalRouteActionAdapter.Actions {
        @Override
        public void onRun(RouteDefinition routeDefinition) {
            openRouteForSimulation(routeDefinition);
        }

        @Override
        public void onEdit(RouteDefinition routeDefinition) {
            openRouteForEditing(routeDefinition);
        }

        @Override
        public void onShare(RouteDefinition routeDefinition) {
            ShareUtils.shareFile(RouteActivity.this, routeDefinition.getFile(), routeDefinition.getName());
        }

        @Override
        public void onDelete(RouteDefinition routeDefinition) {
            confirmDelete(routeDefinition);
        }
    }

    private final class SharedActions implements SharedRouteActionAdapter.Actions {
        @Override
        public void onDownload(SharedRouteSummary routeSummary) {
            downloadSharedRoute(routeSummary, false);
        }

        @Override
        public void onDownloadAndRun(SharedRouteSummary routeSummary) {
            downloadSharedRoute(routeSummary, true);
        }
    }
}
