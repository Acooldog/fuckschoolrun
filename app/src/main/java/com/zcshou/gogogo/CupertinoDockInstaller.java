package com.acooldog.toolbox;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.acooldog.toolbox.ui.CupertinoDockView;

public final class CupertinoDockInstaller {
    public static final int TAB_DRAW = 0;
    public static final int TAB_SIMULATION = 1;
    public static final int TAB_NFC = 2;
    public static final int TAB_MINE = 3;
    private static final String EXTRA_PREVIOUS_TAB = "extra_previous_tab";
    private static final int DOCK_BOTTOM_RESERVE_DP = 96;

    private CupertinoDockInstaller() {
    }

    public static void install(@NonNull Activity activity, int selectedTab) {
        install(activity, selectedTab, false);
    }

    public static void install(
            @NonNull Activity activity,
            int selectedTab,
            boolean simulationPage
    ) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) {
            return;
        }

        CupertinoDockView dockView = new CupertinoDockView(activity);
        int previousTab = activity.getIntent().getIntExtra(EXTRA_PREVIOUS_TAB, selectedTab);
        boolean initialSimulationPage = previousTab == TAB_SIMULATION;
        boolean animateSimulationLabel = previousTab != selectedTab
                && (initialSimulationPage || simulationPage);
        dockView.setTabs(
                previousTab,
                selectedTab,
                initialSimulationPage,
                simulationPage,
                animateSimulationLabel
        );
        dockView.setListener(new CupertinoDockView.Listener() {
            @Override
            public void onTabSelected(int tab) {
                openTab(activity, tab);
            }
        });

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, 96),
                Gravity.BOTTOM
        );
        content.addView(dockView, layoutParams);
    }

    public static void reserveBottomSpace(@NonNull View view) {
        ViewGroup.LayoutParams rawParams = view.getLayoutParams();
        if (rawParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) rawParams;
            params.bottomMargin = dp(view.getContext(), DOCK_BOTTOM_RESERVE_DP);
            view.setLayoutParams(params);
        }
    }

    private static void openTab(@NonNull Activity activity, int tab) {
        if (tab == TAB_SIMULATION && activity instanceof RouteRunActivity) {
            ((RouteRunActivity) activity).openSimulationSettingsFromDock();
            return;
        }

        Class<?> target;
        switch (tab) {
            case TAB_SIMULATION:
                target = RouteRunActivity.class;
                break;
            case TAB_NFC:
                target = NfcToolsActivity.class;
                break;
            case TAB_MINE:
                target = MineActivity.class;
                break;
            case TAB_DRAW:
            default:
                target = HomeActivity.class;
                break;
        }

        if (activity.getClass().equals(target)) {
            return;
        }
        Intent intent = new Intent(activity, target);
        intent.putExtra(EXTRA_PREVIOUS_TAB, currentTab(activity));
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    private static int currentTab(@NonNull Activity activity) {
        if (activity instanceof RouteRunActivity) {
            return TAB_SIMULATION;
        }
        if (activity instanceof NfcToolsActivity) {
            return TAB_NFC;
        }
        if (activity instanceof MineActivity) {
            return TAB_MINE;
        }
        return TAB_DRAW;
    }

    private static int dp(@NonNull Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static int dp(@NonNull android.content.Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
