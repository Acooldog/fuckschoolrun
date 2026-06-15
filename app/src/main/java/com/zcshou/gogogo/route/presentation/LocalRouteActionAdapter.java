package com.acooldog.toolbox.route.presentation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.acooldog.toolbox.R;
import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.utils.GoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LocalRouteActionAdapter extends BaseAdapter {
    public interface Actions {
        void onRun(RouteDefinition routeDefinition);

        void onEdit(RouteDefinition routeDefinition);

        void onShare(RouteDefinition routeDefinition);

        void onDelete(RouteDefinition routeDefinition);
    }

    private final LayoutInflater layoutInflater;
    private final List<RouteDefinition> routes;
    private final Actions actions;

    public LocalRouteActionAdapter(Context context, Actions actions) {
        this.layoutInflater = LayoutInflater.from(context);
        this.routes = new ArrayList<>();
        this.actions = actions;
    }

    public void submit(List<RouteDefinition> newRoutes) {
        routes.clear();
        if (newRoutes != null) {
            routes.addAll(newRoutes);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return routes.size();
    }

    @Override
    public RouteDefinition getItem(int position) {
        return routes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = layoutInflater.inflate(R.layout.route_local_item, parent, false);
        }

        RouteDefinition routeDefinition = getItem(position);
        TextView nameView = view.findViewById(R.id.route_item_name);
        TextView metaView = view.findViewById(R.id.route_item_meta);
        Button runButton = view.findViewById(R.id.btn_route_item_run);
        Button editButton = view.findViewById(R.id.btn_route_item_edit);
        Button shareButton = view.findViewById(R.id.btn_route_item_share);
        Button deleteButton = view.findViewById(R.id.btn_route_item_delete);

        nameView.setText(routeDefinition.getName());
        metaView.setText(buildMeta(routeDefinition));
        runButton.setOnClickListener(v -> actions.onRun(routeDefinition));
        boolean readOnlyPrivacyRoute = routeDefinition.isDownloadedFromShared() && routeDefinition.isPrivacyProtected();
        editButton.setEnabled(!readOnlyPrivacyRoute);
        editButton.setText(readOnlyPrivacyRoute ? R.string.route_item_read_only : R.string.route_item_edit);
        if (readOnlyPrivacyRoute) {
            editButton.setOnClickListener(null);
        } else {
            editButton.setOnClickListener(v -> actions.onEdit(routeDefinition));
        }
        shareButton.setOnClickListener(v -> actions.onShare(routeDefinition));
        deleteButton.setOnClickListener(v -> actions.onDelete(routeDefinition));
        return view;
    }

    private String buildMeta(RouteDefinition routeDefinition) {
        String shareTag;
        if (routeDefinition.shouldMaskMapForSimulation()) {
            shareTag = "[隐私路线] ";
        } else if (routeDefinition.isDownloadedFromShared()) {
            shareTag = "[共享下载] ";
        } else if (routeDefinition.isSharedRoute()) {
            shareTag = "[已共享] ";
        } else {
            shareTag = "[本地路线] ";
        }
        return String.format(
                Locale.getDefault(),
                "%s%d 点 · %s",
                shareTag,
                routeDefinition.getPoints().size(),
                GoUtils.timeStamp2Date(Long.toString(routeDefinition.getUpdatedAt() / 1000L))
        );
    }
}
