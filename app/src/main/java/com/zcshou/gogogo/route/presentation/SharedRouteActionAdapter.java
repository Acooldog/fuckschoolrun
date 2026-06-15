package com.acooldog.toolbox.route.presentation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.acooldog.toolbox.R;
import com.acooldog.toolbox.share.domain.model.SharedRouteSummary;
import com.acooldog.toolbox.utils.GoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SharedRouteActionAdapter extends BaseAdapter {
    public interface Actions {
        void onDownload(SharedRouteSummary routeSummary);

        void onDownloadAndRun(SharedRouteSummary routeSummary);
    }

    private final LayoutInflater layoutInflater;
    private final List<SharedRouteSummary> routes;
    private final Actions actions;

    public SharedRouteActionAdapter(Context context, Actions actions) {
        this.layoutInflater = LayoutInflater.from(context);
        this.routes = new ArrayList<>();
        this.actions = actions;
    }

    public void submit(List<SharedRouteSummary> newRoutes) {
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
    public SharedRouteSummary getItem(int position) {
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
            view = layoutInflater.inflate(R.layout.route_shared_item, parent, false);
        }

        SharedRouteSummary routeSummary = getItem(position);
        TextView nameView = view.findViewById(R.id.route_item_name);
        TextView metaView = view.findViewById(R.id.route_item_meta);
        Button downloadButton = view.findViewById(R.id.btn_route_item_download);
        Button downloadRunButton = view.findViewById(R.id.btn_route_item_download_run);

        nameView.setText(routeSummary.getName());
        metaView.setText(buildMeta(routeSummary));
        if (routeSummary.isPrivacyMode()) {
            downloadButton.setEnabled(false);
            downloadButton.setText(R.string.route_item_privacy_locked);
            downloadRunButton.setText(R.string.route_item_simulate_privacy);
            downloadButton.setOnClickListener(null);
        } else {
            downloadButton.setEnabled(true);
            downloadButton.setText(R.string.route_item_download);
            downloadRunButton.setText(R.string.route_item_download_run);
            downloadButton.setOnClickListener(v -> actions.onDownload(routeSummary));
        }
        downloadRunButton.setOnClickListener(v -> actions.onDownloadAndRun(routeSummary));
        return view;
    }

    private String buildMeta(SharedRouteSummary routeSummary) {
        String privacyTag = routeSummary.isPrivacyMode() ? "[隐私共享] " : "[共享路线] ";
        return String.format(
                Locale.getDefault(),
                "%s%d 点 · %s",
                privacyTag,
                routeSummary.getPointCount(),
                GoUtils.timeStamp2Date(Long.toString(routeSummary.getCreatedAt() / 1000L))
        );
    }
}
