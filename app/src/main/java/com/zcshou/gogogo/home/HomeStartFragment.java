package com.acooldog.toolbox.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.acooldog.toolbox.NfcToolsActivity;
import com.acooldog.toolbox.R;
import com.acooldog.toolbox.RouteActivity;
import com.acooldog.toolbox.RouteCreateActivity;
import com.acooldog.toolbox.RouteRunActivity;
import com.acooldog.toolbox.UsageTipsActivity;

public class HomeStartFragment extends Fragment {
    public static HomeStartFragment newInstance() {
        return new HomeStartFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_start, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindCard(view, R.id.card_nfc_tools, NfcToolsActivity.class);
        bindCard(view, R.id.card_route_run, RouteRunActivity.class);
        bindCard(view, R.id.card_route_create, RouteCreateActivity.class);
        bindCard(view, R.id.card_route_library, RouteActivity.class);
        bindCard(view, R.id.card_usage_tips, UsageTipsActivity.class);
        bindBilibiliCard(view);
    }

    private void bindCard(View root, int viewId, Class<?> activityClass) {
        root.findViewById(viewId).setOnClickListener(v -> {
            if (getContext() != null) {
                startActivity(new Intent(getContext(), activityClass));
            }
        });
    }

    private void bindBilibiliCard(View root) {
        root.findViewById(R.id.card_bilibili).setOnClickListener(v -> {
            if (getContext() == null) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_bilibili_url)));
            startActivity(intent);
        });
    }
}
