package com.zcshou.gogogo.route.presentation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.zcshou.gogogo.R;
import com.zcshou.gogogo.route.domain.model.RouteDefinition;
import com.zcshou.utils.GoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RouteListAdapter extends BaseAdapter {
    private final LayoutInflater layoutInflater;
    private final List<RouteDefinition> routes;

    public RouteListAdapter(Context context) {
        layoutInflater = LayoutInflater.from(context);
        routes = new ArrayList<>();
    }

    public void submit(List<RouteDefinition> newRoutes) {
        routes.clear();
        routes.addAll(newRoutes);
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
            view = layoutInflater.inflate(R.layout.route_item, parent, false);
        }

        RouteDefinition routeDefinition = getItem(position);
        TextView nameView = view.findViewById(R.id.route_item_name);
        TextView metaView = view.findViewById(R.id.route_item_meta);

        nameView.setText(routeDefinition.getName());
        metaView.setText(String.format(
                Locale.getDefault(),
                "%d 点 · %s",
                routeDefinition.getPoints().size(),
                GoUtils.timeStamp2Date(Long.toString(routeDefinition.getUpdatedAt() / 1000L))
        ));
        return view;
    }
}
