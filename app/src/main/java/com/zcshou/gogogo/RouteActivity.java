package com.zcshou.gogogo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.zcshou.gogogo.route.domain.model.RouteDefinition;
import com.zcshou.gogogo.route.presentation.RouteListAdapter;
import com.zcshou.gogogo.route.presentation.RouteListViewModel;
import com.zcshou.utils.GoUtils;
import com.zcshou.utils.ShareUtils;

public class RouteActivity extends BaseActivity {
    private RouteListViewModel viewModel;
    private RouteListAdapter routeListAdapter;
    private TextView emptyView;

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
        routeListAdapter = new RouteListAdapter(this);

        emptyView = findViewById(R.id.route_no_data);
        ListView routeListView = findViewById(R.id.route_list_view);
        routeListView.setAdapter(routeListAdapter);
        routeListView.setOnItemClickListener((parent, view, position, id) -> openRouteForSimulation(routeListAdapter.getItem(position)));
        routeListView.setOnItemLongClickListener((parent, view, position, id) -> {
            showRouteActions(routeListAdapter.getItem(position));
            return true;
        });

        FloatingActionButton importButton = findViewById(R.id.fab_import);
        importButton.setOnClickListener(v -> importLauncher.launch(new String[]{"application/json", "*/*"}));

        observeRoutes();
        refreshRoutes();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void observeRoutes() {
        viewModel.getRoutes().observe(this, routes -> {
            routeListAdapter.submit(routes);
            emptyView.setVisibility(routes == null || routes.isEmpty() ? TextView.VISIBLE : TextView.GONE);
        });
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

    private void showRouteActions(RouteDefinition routeDefinition) {
        String[] actions = {"分享路线", "删除路线"};
        new AlertDialog.Builder(this)
                .setTitle(routeDefinition.getName())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        ShareUtils.shareFile(this, routeDefinition.getFile(), routeDefinition.getName());
                    } else {
                        confirmDelete(routeDefinition);
                    }
                })
                .show();
    }

    private void confirmDelete(RouteDefinition routeDefinition) {
        new AlertDialog.Builder(this)
                .setTitle("删除路线")
                .setMessage("确定要删除这条路线吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    try {
                        viewModel.deleteRoute(routeDefinition);
                        GoUtils.DisplayToast(this, "路线已删除");
                    } catch (Exception exception) {
                        GoUtils.DisplayToast(this, "删除路线失败");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
